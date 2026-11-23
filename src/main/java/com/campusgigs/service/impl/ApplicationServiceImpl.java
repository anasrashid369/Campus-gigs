package com.campusgigs.service.impl;

import com.campusgigs.dao.ApplicationDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigApplication;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.AppEvent;
import com.campusgigs.pattern.observer.ApplicationSubmitted;
import com.campusgigs.pattern.observer.ApplicationWithdrawn;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.SettingsService;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Default {@link ApplicationService}. */
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationDao applicationDao;
    private final GigDao gigDao;
    private final UserDao userDao;
    private final SettingsService settingsService;
    private final GigLifecycleService lifecycleService;
    private final TransactionManager transactions;
    private final EventBus eventBus;

    public ApplicationServiceImpl(ApplicationDao applicationDao, GigDao gigDao, UserDao userDao,
                                  SettingsService settingsService, GigLifecycleService lifecycleService,
                                  TransactionManager transactions, EventBus eventBus) {
        this.applicationDao = Objects.requireNonNull(applicationDao, "applicationDao");
        this.gigDao = Objects.requireNonNull(gigDao, "gigDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.lifecycleService = Objects.requireNonNull(lifecycleService, "lifecycleService");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    /** A change that was saved, and the event to announce once it is committed. */
    private record Outcome(ApplicationDto result, AppEvent event) {
    }

    @Override
    public ApplicationDto apply(long freelancerId, long gigId, String proposalMessage) {
        String proposal = proposalMessage == null ? "" : proposalMessage.trim();
        if (proposal.length() < MIN_PROPOSAL_LENGTH || proposal.length() > MAX_PROPOSAL_LENGTH) {
            throw new ValidationException("Your proposal must be " + MIN_PROPOSAL_LENGTH + " to "
                    + MAX_PROPOSAL_LENGTH + " characters.");
        }
        User freelancer = requireActive(freelancerId, Role.FREELANCER, "apply to a gig");

        return publish(transactions.call(() -> {
            Gig gig = lockGig(gigId);
            if (gig.status() != GigStatus.OPEN) {
                throw new ValidationException("This gig is no longer open for applications.");
            }
            GigApplication earlier = applicationDao.findByGigAndFreelancer(gigId, freelancerId).orElse(null);
            if (earlier != null && earlier.status() != ApplicationStatus.WITHDRAWN) {
                throw new ValidationException("You already applied to this gig.");
            }
            int limit = settingsService.getMaxApplicationsPerFreelancer();
            if (applicationDao.countByFreelancerAndStatus(freelancerId, ApplicationStatus.PENDING) >= limit) {
                throw new ValidationException("You can have at most " + limit
                        + " pending applications. Withdraw one first.");
            }
            long applicationId;
            if (earlier == null) {
                applicationId = applicationDao.insert(gigId, freelancerId, proposal);
            } else {
                applicationId = earlier.applicationId();
                applicationDao.reopen(applicationId, proposal);
            }
            return new Outcome(toDto(load(applicationId), gig, freelancer),
                    new ApplicationSubmitted(applicationId, gigId, gig.clientId(), freelancerId));
        }));
    }

    @Override
    public ApplicationDto withdraw(long freelancerId, long applicationId) {
        User freelancer = requireActive(freelancerId, Role.FREELANCER, "withdraw an application");

        return publish(transactions.call(() -> {
            GigApplication first = load(applicationId);
            Gig gig = lockGig(first.gigId());
            GigApplication application = load(applicationId);
            if (application.freelancerId() != freelancerId) {
                throw new ValidationException("You can only withdraw your own applications.");
            }
            if (application.status() != ApplicationStatus.PENDING) {
                throw new ValidationException("Only a pending application can be withdrawn.");
            }
            applicationDao.updateStatus(applicationId, ApplicationStatus.WITHDRAWN);
            return new Outcome(toDto(load(applicationId), gig, freelancer),
                    new ApplicationWithdrawn(applicationId, gig.gigId(), gig.clientId(), freelancerId));
        }));
    }

    @Override
    public List<ApplicationDto> listForGig(long clientId, long gigId) {
        requireActive(clientId, Role.CLIENT, "see applications");
        Gig gig = gigDao.findById(gigId).orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
        if (gig.clientId() != clientId) {
            throw new ValidationException("Only the client who posted the gig can see its applications.");
        }
        return applicationDao.findByGigId(gigId).stream()
                .map(application -> toDto(application, gig, findUser(application.freelancerId())))
                .toList();
    }

    @Override
    public List<ApplicationDto> listForFreelancer(long freelancerId) {
        User freelancer = requireActive(freelancerId, Role.FREELANCER, "see applications");
        return applicationDao.findByFreelancerId(freelancerId).stream()
                .map(application -> toDto(application, findGig(application.gigId()), freelancer))
                .toList();
    }

    @Override
    public GigDto hire(long clientId, long gigId, long applicationId) {
        requireActive(clientId, Role.CLIENT, "hire a freelancer");
        return transactions.call(() -> {
            Gig gig = lockGig(gigId);
            if (gig.clientId() != clientId) {
                throw new ValidationException("Only the client who posted the gig can hire for it.");
            }
            GigApplication application = load(applicationId);
            if (application.gigId() != gigId) {
                throw new ValidationException("This application does not belong to the gig.");
            }
            if (application.status() != ApplicationStatus.PENDING) {
                throw new ValidationException("Only a pending application can be accepted.");
            }
            applicationDao.updateStatus(applicationId, ApplicationStatus.ACCEPTED);
            applicationDao.rejectPendingExcept(gigId, applicationId);
            // Last on purpose: it moves the money and publishes FreelancerHired. If it fails, the
            // two updates above are rolled back with it.
            return lifecycleService.hire(clientId, gigId, application.freelancerId());
        });
    }

    private GigApplication load(long applicationId) {
        return applicationDao.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private Gig lockGig(long gigId) {
        return gigDao.findByIdForUpdate(gigId).orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
    }

    private Gig findGig(long gigId) {
        return gigDao.findById(gigId).orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
    }

    private User findUser(long userId) {
        return userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User requireActive(long userId, Role role, String action) {
        User user = findUser(userId);
        if (user.role() != role || user.accountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Only an active " + role.name().toLowerCase(Locale.ROOT)
                    + " can " + action + ".");
        }
        return user;
    }

    private static ApplicationDto toDto(GigApplication application, Gig gig, User freelancer) {
        return new ApplicationDto(application.applicationId(), gig.gigId(), gig.title(), gig.status(),
                freelancer.userId(), freelancer.fullName(), application.proposalMessage(), application.status(),
                application.submittedAt());
    }

    /** Announces the event of a change that has been committed and returns the change's result. */
    private ApplicationDto publish(Outcome outcome) {
        eventBus.publish(outcome.event());
        return outcome.result();
    }
}
