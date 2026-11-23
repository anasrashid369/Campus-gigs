package com.campusgigs.service.impl;

import com.campusgigs.dao.DisputeDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.DisputeDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.PayoutDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Dispute;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.AppEvent;
import com.campusgigs.pattern.observer.DisputeRaised;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.observer.FreelancerHired;
import com.campusgigs.pattern.observer.RevisionRequested;
import com.campusgigs.pattern.observer.WorkApproved;
import com.campusgigs.pattern.observer.WorkSubmitted;
import com.campusgigs.pattern.state.GigAction;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.service.EscrowService;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.SettingsService;

import java.util.Objects;

/** Default {@link GigLifecycleService}. */
public class GigLifecycleServiceImpl implements GigLifecycleService {

    private final GigDao gigDao;
    private final DisputeDao disputeDao;
    private final UserDao userDao;
    private final EscrowService escrowService;
    private final SettingsService settingsService;
    private final GigStateMachine stateMachine;
    private final TransactionManager transactions;
    private final EventBus eventBus;

    public GigLifecycleServiceImpl(GigDao gigDao, DisputeDao disputeDao, UserDao userDao,
                                   EscrowService escrowService, SettingsService settingsService,
                                   GigStateMachine stateMachine, TransactionManager transactions,
                                   EventBus eventBus) {
        this.gigDao = Objects.requireNonNull(gigDao, "gigDao");
        this.disputeDao = Objects.requireNonNull(disputeDao, "disputeDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.escrowService = Objects.requireNonNull(escrowService, "escrowService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    /** A change that was saved, and the event to announce once it is committed. */
    private record Outcome<T>(T result, AppEvent event) {
    }

    @Override
    public GigDto hire(long clientId, long gigId, long freelancerId) {
        return publish(transactions.call(() -> {
            Gig gig = lockGig(gigId);
            requireOwner(gig, clientId, "hire a freelancer for");
            User freelancer = userDao.findById(freelancerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
            if (freelancer.role() != Role.FREELANCER || freelancer.accountStatus() != AccountStatus.ACTIVE) {
                throw new ValidationException("Only an active freelancer can be hired");
            }
            GigStatus next = stateMachine.transition(gig.status(), GigAction.HIRE);
            escrowService.holdInEscrow(clientId, gigId, gig.budget());
            gigDao.assign(gigId, freelancerId, next);
            return new Outcome<>(GigMapper.toDto(gig, next, freelancerId),
                    new FreelancerHired(gigId, clientId, freelancerId, gig.budget()));
        }));
    }

    @Override
    public GigDto submitWork(long freelancerId, long gigId) {
        return publish(transactions.call(() -> {
            Gig gig = lockGig(gigId);
            requireRole(freelancerId, Role.FREELANCER, "submit work");
            if (gig.assignedFreelancerId() == null || gig.assignedFreelancerId() != freelancerId) {
                throw new ValidationException("Only the freelancer hired for this gig can submit work");
            }
            GigStatus next = stateMachine.transition(gig.status(), GigAction.SUBMIT);
            gigDao.updateStatus(gigId, next);
            return new Outcome<>(GigMapper.toDto(gig, next, gig.assignedFreelancerId()),
                    new WorkSubmitted(gigId, gig.clientId(), freelancerId));
        }));
    }

    @Override
    public GigDto approve(long clientId, long gigId) {
        return publish(transactions.call(() -> {
            Gig gig = lockGig(gigId);
            requireOwner(gig, clientId, "approve work on");
            GigStatus next = stateMachine.transition(gig.status(), GigAction.APPROVE);
            long freelancerId = hiredFreelancer(gig);
            PayoutDto payout = escrowService.releaseToFreelancer(clientId, freelancerId, gigId,
                    gig.budget(), settingsService.getPlatformFeePercent());
            gigDao.updateStatus(gigId, next);
            return new Outcome<>(GigMapper.toDto(gig, next, freelancerId),
                    new WorkApproved(gigId, clientId, freelancerId, payout.netAmount()));
        }));
    }

    @Override
    public GigDto requestRevision(long clientId, long gigId) {
        return publish(transactions.call(() -> {
            Gig gig = lockGig(gigId);
            requireOwner(gig, clientId, "request a revision of");
            GigStatus next = stateMachine.transition(gig.status(), GigAction.REQUEST_REVISION);
            gigDao.updateStatus(gigId, next);
            long freelancerId = hiredFreelancer(gig);
            return new Outcome<>(GigMapper.toDto(gig, next, freelancerId),
                    new RevisionRequested(gigId, clientId, freelancerId));
        }));
    }

    @Override
    public DisputeDto raiseDispute(long userId, long gigId, String reason) {
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.isEmpty()) {
            throw new ValidationException("Please describe the problem");
        }
        if (trimmedReason.length() > MAX_DISPUTE_REASON_LENGTH) {
            throw new ValidationException("The description can be at most "
                    + MAX_DISPUTE_REASON_LENGTH + " characters");
        }
        return publish(transactions.call(() -> {
            Gig gig = lockGig(gigId);
            User user = userDao.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean isClient = user.role() == Role.CLIENT && gig.clientId() == userId;
            boolean isFreelancer = user.role() == Role.FREELANCER && gig.assignedFreelancerId() != null
                    && gig.assignedFreelancerId() == userId;
            if (!isClient && !isFreelancer) {
                throw new ValidationException("Only the client or the hired freelancer can dispute this gig");
            }
            GigStatus next = stateMachine.transition(gig.status(), GigAction.RAISE_DISPUTE);
            Dispute dispute = disputeDao.insert(gigId, userId, trimmedReason);
            gigDao.updateStatus(gigId, next);
            long freelancerId = hiredFreelancer(gig);
            Gig disputed = new Gig(gig.gigId(), gig.clientId(), gig.categoryId(), freelancerId, gig.title(),
                    gig.description(), gig.budget(), gig.deadline(), next, gig.createdAt());
            return new Outcome<>(DisputeMapper.toDto(dispute, disputed),
                    new DisputeRaised(dispute.disputeId(), gigId, gig.clientId(), freelancerId, userId));
        }));
    }

    private Gig lockGig(long gigId) {
        return gigDao.findByIdForUpdate(gigId)
                .orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
    }

    private void requireRole(long userId, Role role, String action) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.role() != role) {
            throw new ValidationException("Only a " + role.name().toLowerCase(java.util.Locale.ROOT)
                    + " can " + action + " a gig");
        }
    }

    /** The caller must be a CLIENT and the client who posted the gig. */
    private void requireOwner(Gig gig, long clientId, String action) {
        requireRole(clientId, Role.CLIENT, action);
        if (gig.clientId() != clientId) {
            throw new ValidationException("Only the client who posted the gig can " + action + " it");
        }
    }

    private static long hiredFreelancer(Gig gig) {
        if (gig.assignedFreelancerId() == null) {
            throw new IllegalStateException("Gig " + gig.gigId() + " has no hired freelancer");
        }
        return gig.assignedFreelancerId();
    }

    /** Announces the event of a change that has been committed and returns the change's result. */
    private <T> T publish(Outcome<T> outcome) {
        eventBus.publish(outcome.event());
        return outcome.result();
    }
}
