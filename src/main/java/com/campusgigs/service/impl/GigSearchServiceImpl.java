package com.campusgigs.service.impl;

import com.campusgigs.dao.FreelancerProfileDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigFilter;
import com.campusgigs.dto.GigSort;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.Skill;
import com.campusgigs.model.User;
import com.campusgigs.pattern.strategy.GigRankingStrategy;
import com.campusgigs.pattern.strategy.RankingContext;
import com.campusgigs.service.GigSearchService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default {@link GigSearchService}. The ordering is chosen from the injected map of strategies,
 * so a new ranking needs no change here.
 */
public class GigSearchServiceImpl implements GigSearchService {

    private final GigDao gigDao;
    private final UserDao userDao;
    private final FreelancerProfileDao profileDao;
    private final Map<GigSort, GigRankingStrategy> strategies;

    /**
     * @param strategies one ranking for every {@link GigSort} value
     * @throws IllegalArgumentException if a sort has no strategy
     */
    public GigSearchServiceImpl(GigDao gigDao, UserDao userDao, FreelancerProfileDao profileDao,
                                Map<GigSort, GigRankingStrategy> strategies) {
        this.gigDao = Objects.requireNonNull(gigDao, "gigDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.profileDao = Objects.requireNonNull(profileDao, "profileDao");
        this.strategies = new EnumMap<>(GigSort.class);
        this.strategies.putAll(Objects.requireNonNull(strategies, "strategies"));
        for (GigSort sort : GigSort.values()) {
            if (!this.strategies.containsKey(sort)) {
                throw new IllegalArgumentException("No ranking strategy for " + sort);
            }
        }
    }

    @Override
    public List<GigDto> search(long freelancerId, GigFilter filter) {
        User user = userDao.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.role() != Role.FREELANCER || user.accountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Only an active freelancer can search for gigs.");
        }
        GigFilter wanted = filter == null ? GigFilter.none() : filter;
        validate(wanted);

        List<GigDto> found = gigDao.searchOpen(wanted.keyword(), wanted.categoryId(), wanted.minBudget(),
                wanted.maxBudget()).stream().map(GigMapper::toDto).toList();
        GigRankingStrategy strategy = strategies.get(wanted.sort());
        return strategy.rank(found, strategy.needsSkillContext() ? skillContext(freelancerId, found)
                : RankingContext.empty());
    }

    private RankingContext skillContext(long freelancerId, List<GigDto> gigs) {
        Set<Long> freelancerSkills = profileDao.findSkills(freelancerId).stream()
                .map(Skill::skillId).collect(Collectors.toSet());
        Map<Long, Set<Long>> gigSkills = gigDao.findSkillIdsByGigIds(gigs.stream().map(GigDto::gigId).toList());
        return new RankingContext(freelancerSkills, gigSkills);
    }

    private static void validate(GigFilter filter) {
        if (filter.keyword() != null && filter.keyword().length() > MAX_KEYWORD_LENGTH) {
            throw new ValidationException("The search word can be at most " + MAX_KEYWORD_LENGTH + " characters.");
        }
        if ((filter.minBudget() != null && filter.minBudget() < 0)
                || (filter.maxBudget() != null && filter.maxBudget() < 0)) {
            throw new ValidationException("The budget limits cannot be negative.");
        }
        if (filter.minBudget() != null && filter.maxBudget() != null && filter.minBudget() > filter.maxBudget()) {
            throw new ValidationException("The minimum budget cannot be above the maximum budget.");
        }
    }
}
