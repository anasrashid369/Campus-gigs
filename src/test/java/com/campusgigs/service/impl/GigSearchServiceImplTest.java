package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.FreelancerProfileDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigFilter;
import com.campusgigs.dto.GigSort;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.Skill;
import com.campusgigs.model.User;
import com.campusgigs.pattern.strategy.DeadlineSoonestRanking;
import com.campusgigs.pattern.strategy.GigRankingStrategy;
import com.campusgigs.pattern.strategy.HighestBudgetRanking;
import com.campusgigs.pattern.strategy.NewestFirstRanking;
import com.campusgigs.pattern.strategy.SkillMatchRanking;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GigSearchServiceImplTest {

    private static final long FREELANCER = 3;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 12, 0);

    @Mock GigDao gigDao;
    @Mock UserDao userDao;
    @Mock FreelancerProfileDao profileDao;

    private GigSearchServiceImpl service;

    private static Map<GigSort, GigRankingStrategy> allStrategies() {
        Map<GigSort, GigRankingStrategy> map = new EnumMap<>(GigSort.class);
        map.put(GigSort.NEWEST, new NewestFirstRanking());
        map.put(GigSort.BUDGET_HIGH, new HighestBudgetRanking());
        map.put(GigSort.DEADLINE_SOON, new DeadlineSoonestRanking());
        map.put(GigSort.SKILL_MATCH, new SkillMatchRanking());
        return map;
    }

    @BeforeEach
    void setUp() {
        service = new GigSearchServiceImpl(gigDao, userDao, profileDao, allStrategies());
        lenient().when(userDao.findById(FREELANCER)).thenReturn(Optional.of(user(FREELANCER, Role.FREELANCER, AccountStatus.ACTIVE)));
    }

    private static User user(long id, Role role, AccountStatus status) {
        return new User(id, role, "User " + id, "u" + id + "@example.com", "hash", status, NOW);
    }

    private static Gig gig(long id, long budget, int daysOld) {
        return new Gig(id, 1, 1, null, "Gig " + id, "Description", budget, LocalDate.of(2026, 12, 31), GigStatus.OPEN,
                NOW.minusDays(daysOld));
    }

    private static List<Long> ids(List<GigDto> gigs) {
        return gigs.stream().map(GigDto::gigId).toList();
    }

    @Test
    void searchPassesTheConditionsToTheDaoAndRanksNewestFirstByDefault() {
        when(gigDao.searchOpen("logo", 7L, 100L, 900L)).thenReturn(List.of(gig(1, 300, 5), gig(2, 200, 1)));

        List<GigDto> result = service.search(FREELANCER, new GigFilter("  logo ", 7L, 100L, 900L, null));

        assertEquals(List.of(2L, 1L), ids(result));
    }

    @Test
    void aNullFilterMeansEverythingNewestFirst() {
        when(gigDao.searchOpen(null, null, null, null)).thenReturn(List.of(gig(1, 300, 5), gig(2, 200, 1)));

        assertEquals(List.of(2L, 1L), ids(service.search(FREELANCER, null)));
    }

    @Test
    void theChosenSortDecidesTheOrder() {
        when(gigDao.searchOpen(null, null, null, null)).thenReturn(List.of(gig(1, 300, 5), gig(2, 200, 1), gig(3, 900, 9)));

        List<GigDto> result = service.search(FREELANCER, new GigFilter(null, null, null, null, GigSort.BUDGET_HIGH));

        assertEquals(List.of(3L, 1L, 2L), ids(result));
        verifyNoInteractions(profileDao);
        verify(gigDao, never()).findSkillIdsByGigIds(any());
    }

    @Test
    void skillMatchLoadsTheSkillsOfTheFreelancerAndOfTheGigs() {
        when(gigDao.searchOpen(null, null, null, null)).thenReturn(List.of(gig(1, 300, 5), gig(2, 200, 1)));
        when(profileDao.findSkills(FREELANCER)).thenReturn(List.of(new Skill(10, "Java")));
        when(gigDao.findSkillIdsByGigIds(List.of(1L, 2L))).thenReturn(Map.of(1L, Set.of(10L)));

        List<GigDto> result = service.search(FREELANCER, new GigFilter(null, null, null, null, GigSort.SKILL_MATCH));

        assertEquals(List.of(1L, 2L), ids(result));
    }

    @Test
    void noResultsIsNotAnError() {
        when(gigDao.searchOpen(null, null, null, null)).thenReturn(List.of());

        assertEquals(List.of(), service.search(FREELANCER, null));
    }

    @Test
    void badFiltersAreRejectedBeforeTheDatabaseIsAsked() {
        assertThrows(ValidationException.class,
                () -> service.search(FREELANCER, new GigFilter("x".repeat(101), null, null, null, null)));
        assertThrows(ValidationException.class,
                () -> service.search(FREELANCER, new GigFilter(null, null, -1L, null, null)));
        assertThrows(ValidationException.class,
                () -> service.search(FREELANCER, new GigFilter(null, null, null, -5L, null)));
        assertThrows(ValidationException.class,
                () -> service.search(FREELANCER, new GigFilter(null, null, 500L, 100L, null)));
        verify(gigDao, never()).searchOpen(any(), any(), any(), any());
    }

    @Test
    void equalMinimumAndMaximumIsFine() {
        when(gigDao.searchOpen(null, null, 300L, 300L)).thenReturn(List.of());

        service.search(FREELANCER, new GigFilter(null, null, 300L, 300L, null));
    }

    @Test
    void onlyAnActiveFreelancerCanSearch() {
        when(userDao.findById(1)).thenReturn(Optional.of(user(1, Role.CLIENT, AccountStatus.ACTIVE)));
        when(userDao.findById(5)).thenReturn(Optional.of(user(5, Role.FREELANCER, AccountStatus.SUSPENDED)));
        when(userDao.findById(9)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.search(1, null));
        assertThrows(ValidationException.class, () -> service.search(5, null));
        assertThrows(ResourceNotFoundException.class, () -> service.search(9, null));
        verify(gigDao, never()).searchOpen(any(), any(), any(), any());
    }

    @Test
    void everySortNeedsAStrategy() {
        Map<GigSort, GigRankingStrategy> incomplete = new EnumMap<>(GigSort.class);
        incomplete.put(GigSort.NEWEST, new NewestFirstRanking());

        assertThrows(IllegalArgumentException.class,
                () -> new GigSearchServiceImpl(gigDao, userDao, profileDao, incomplete));
    }

    @Test
    void aStrategyCanBeReplacedWithoutChangingTheService() {
        Map<GigSort, GigRankingStrategy> custom = allStrategies();
        custom.put(GigSort.NEWEST, (gigs, context) -> gigs.stream().sorted((a, b) -> Long.compare(a.gigId(), b.gigId())).toList());
        GigSearchServiceImpl withCustom = new GigSearchServiceImpl(gigDao, userDao, profileDao, custom);
        when(gigDao.searchOpen(null, null, null, null)).thenReturn(List.of(gig(2, 1, 0), gig(1, 1, 5)));

        assertEquals(List.of(1L, 2L), ids(withCustom.search(FREELANCER, null)));
        verify(gigDao, never()).findSkillIdsByGigIds(any());
        verify(profileDao, never()).findSkills(anyLong());
    }
}
