package com.campusgigs.service.impl;

import com.campusgigs.dao.CategoryDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.SkillDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigRequest;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigCategory;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.state.GigAction;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.service.GigService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Default {@link GigService}. */
public class GigServiceImpl implements GigService {

    static final int MIN_TITLE = 3;
    static final int MAX_TITLE = 100;
    static final int MIN_DESCRIPTION = 10;
    static final int MAX_DESCRIPTION = 2000;
    static final long MIN_BUDGET = 1;
    static final long MAX_BUDGET = 100_000;
    static final int MAX_SKILLS = 10;

    private final GigDao gigDao;
    private final CategoryDao categoryDao;
    private final SkillDao skillDao;
    private final UserDao userDao;
    private final GigStateMachine stateMachine;
    private final TransactionManager transactions;
    private final Clock clock;

    public GigServiceImpl(GigDao gigDao, CategoryDao categoryDao, SkillDao skillDao, UserDao userDao,
                          GigStateMachine stateMachine, TransactionManager transactions, Clock clock) {
        this.gigDao = Objects.requireNonNull(gigDao, "gigDao");
        this.categoryDao = Objects.requireNonNull(categoryDao, "categoryDao");
        this.skillDao = Objects.requireNonNull(skillDao, "skillDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public GigDto createGig(long clientId, GigRequest request) {
        requireActiveUser(clientId, Role.CLIENT, "post a gig");
        ValidatedGig gig = validate(request);
        return transactions.call(() -> {
            long gigId = gigDao.insert(clientId, gig.categoryId(), gig.title(), gig.description(),
                    gig.budget(), gig.deadline());
            gigDao.replaceSkills(gigId, gig.skillIds());
            return GigMapper.toDto(gigDao.findById(gigId)
                    .orElseThrow(() -> new ResourceNotFoundException("Gig not found")));
        });
    }

    @Override
    public GigDto updateGig(long clientId, long gigId, GigRequest request) {
        requireActiveUser(clientId, Role.CLIENT, "change a gig");
        ValidatedGig changes = validate(request);
        return transactions.call(() -> {
            Gig gig = lockGig(gigId);
            requireOwner(gig, clientId);
            if (gig.status() != GigStatus.OPEN) {
                throw new ValidationException("Only open gigs can be edited.");
            }
            gigDao.update(gigId, changes.categoryId(), changes.title(), changes.description(),
                    changes.budget(), changes.deadline());
            gigDao.replaceSkills(gigId, changes.skillIds());
            return GigMapper.toDto(gigDao.findById(gigId)
                    .orElseThrow(() -> new ResourceNotFoundException("Gig not found")));
        });
    }

    @Override
    public GigDto cancelGig(long clientId, long gigId) {
        requireActiveUser(clientId, Role.CLIENT, "cancel a gig");
        return transactions.call(() -> {
            Gig gig = lockGig(gigId);
            requireOwner(gig, clientId);
            GigStatus next = stateMachine.transition(gig.status(), GigAction.CANCEL);
            gigDao.updateStatus(gigId, next);
            return GigMapper.toDto(gig, next, gig.assignedFreelancerId());
        });
    }

    @Override
    public GigDto getGig(long userId, long gigId) {
        Gig gig = gigDao.findById(gigId).orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
        if (gig.status() != GigStatus.OPEN) {
            User user = userDao.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean allowed = user.role() == Role.ADMIN || gig.clientId() == userId
                    || (gig.assignedFreelancerId() != null && gig.assignedFreelancerId() == userId);
            if (!allowed) {
                throw new ValidationException("You cannot view this gig.");
            }
        }
        return GigMapper.toDto(gig);
    }

    @Override
    public List<GigDto> listClientGigs(long clientId) {
        requireActiveUser(clientId, Role.CLIENT, "list gigs");
        return gigDao.findByClientId(clientId).stream().map(GigMapper::toDto).toList();
    }

    @Override
    public List<GigDto> listFreelancerGigs(long freelancerId) {
        requireActiveUser(freelancerId, Role.FREELANCER, "list hired gigs");
        return gigDao.findByFreelancerId(freelancerId).stream().map(GigMapper::toDto).toList();
    }

    @Override
    public List<GigDto> listOpenGigs() {
        return gigDao.findByStatus(GigStatus.OPEN).stream().map(GigMapper::toDto).toList();
    }

    @Override
    public List<SkillDto> getGigSkills(long gigId) {
        return gigDao.findSkills(gigId).stream().map(skill -> new SkillDto(skill.skillId(), skill.name())).toList();
    }

    @Override
    public List<CategoryDto> getActiveCategories() {
        return categoryDao.findAll().stream()
                .filter(GigCategory::active)
                .map(category -> new CategoryDto(category.categoryId(), category.name(), category.active()))
                .toList();
    }

    @Override
    public Set<GigAction> allowedActions(GigStatus status) {
        return stateMachine.allowedActions(status);
    }

    /** The fields of a request after trimming and checking. */
    private record ValidatedGig(String title, String description, long categoryId, long budget,
                                LocalDate deadline, List<Long> skillIds) {
    }

    private ValidatedGig validate(GigRequest request) {
        Objects.requireNonNull(request, "request");
        String title = request.title() == null ? "" : request.title().trim();
        String description = request.description() == null ? "" : request.description().trim();
        if (title.length() < MIN_TITLE || title.length() > MAX_TITLE) {
            throw new ValidationException("The title must be " + MIN_TITLE + " to " + MAX_TITLE + " characters.");
        }
        if (description.length() < MIN_DESCRIPTION || description.length() > MAX_DESCRIPTION) {
            throw new ValidationException("The description must be " + MIN_DESCRIPTION + " to "
                    + MAX_DESCRIPTION + " characters.");
        }
        if (request.budget() < MIN_BUDGET || request.budget() > MAX_BUDGET) {
            throw new ValidationException("The budget must be between " + MIN_BUDGET + " and " + MAX_BUDGET
                    + " tokens.");
        }
        if (request.deadline() == null || request.deadline().isBefore(LocalDate.now(clock))) {
            throw new ValidationException("The deadline cannot be in the past.");
        }
        GigCategory category = categoryDao.findById(request.categoryId())
                .orElseThrow(() -> new ValidationException("Please choose a valid category."));
        if (!category.active()) {
            throw new ValidationException("This category is no longer available.");
        }
        List<Long> skillIds = checkSkills(request.skillIds());
        return new ValidatedGig(title, description, request.categoryId(), request.budget(),
                request.deadline(), skillIds);
    }

    private List<Long> checkSkills(List<Long> skillIds) {
        Set<Long> unique = new LinkedHashSet<>(skillIds);
        if (unique.size() > MAX_SKILLS) {
            throw new ValidationException("You can choose at most " + MAX_SKILLS + " skills.");
        }
        for (long skillId : unique) {
            if (skillDao.findById(skillId).isEmpty()) {
                throw new ValidationException("One of the chosen skills does not exist.");
            }
        }
        return List.copyOf(unique);
    }

    private Gig lockGig(long gigId) {
        return gigDao.findByIdForUpdate(gigId).orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
    }

    private void requireActiveUser(long userId, Role role, String action) {
        User user = userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.role() != role || user.accountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Only an active " + role.name().toLowerCase(Locale.ROOT)
                    + " can " + action + ".");
        }
    }

    private static void requireOwner(Gig gig, long clientId) {
        if (gig.clientId() != clientId) {
            throw new ValidationException("Only the client who posted the gig can change it.");
        }
    }
}
