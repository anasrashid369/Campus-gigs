package com.campusgigs.pattern.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.dto.GigDto;
import com.campusgigs.model.GigStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GigRankingStrategiesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 12, 0);

    private static GigDto gig(long id, long budget, LocalDate deadline, int daysOld) {
        return new GigDto(id, 1, 1, null, "Gig " + id, "Description", budget, deadline, GigStatus.OPEN,
                NOW.minusDays(daysOld));
    }

    private static List<Long> ids(List<GigDto> gigs) {
        return gigs.stream().map(GigDto::gigId).toList();
    }

    private final GigDto oldCheap = gig(1, 100, LocalDate.of(2026, 12, 20), 10);
    private final GigDto newRich = gig(2, 900, LocalDate.of(2026, 12, 30), 1);
    private final GigDto midSoon = gig(3, 500, LocalDate.of(2026, 12, 1), 5);

    @Test
    void newestFirstOrdersByCreationTime() {
        assertEquals(List.of(2L, 3L, 1L), ids(new NewestFirstRanking().rank(List.of(oldCheap, newRich, midSoon),
                RankingContext.empty())));
    }

    @Test
    void highestBudgetOrdersByBudgetThenNewest() {
        GigDto sameBudgetNewer = gig(4, 500, LocalDate.of(2027, 1, 1), 0);

        List<GigDto> ranked = new HighestBudgetRanking().rank(List.of(oldCheap, midSoon, newRich, sameBudgetNewer),
                RankingContext.empty());

        assertEquals(List.of(2L, 4L, 3L, 1L), ids(ranked));
    }

    @Test
    void deadlineSoonestOrdersByDeadlineThenNewest() {
        GigDto sameDeadlineNewer = gig(4, 10, LocalDate.of(2026, 12, 1), 0);

        List<GigDto> ranked = new DeadlineSoonestRanking().rank(List.of(oldCheap, newRich, midSoon, sameDeadlineNewer),
                RankingContext.empty());

        assertEquals(List.of(4L, 3L, 1L, 2L), ids(ranked));
    }

    @Test
    void skillMatchPutsTheBestFitFirstAndBreaksTiesByNewest() {
        RankingContext context = new RankingContext(Set.of(10L, 11L),
                Map.of(1L, Set.of(10L, 11L, 12L), 2L, Set.of(10L), 3L, Set.of(99L)));

        List<GigDto> ranked = new SkillMatchRanking().rank(List.of(midSoon, newRich, oldCheap), context);

        // gig 1 matches two skills, gig 2 one, gig 3 none
        assertEquals(List.of(1L, 2L, 3L), ids(ranked));
    }

    @Test
    void gigsWithTheSameMatchCountKeepTheNewestFirstOrder() {
        RankingContext context = new RankingContext(Set.of(10L), Map.of());

        assertEquals(List.of(2L, 3L, 1L), ids(new SkillMatchRanking().rank(List.of(oldCheap, newRich, midSoon), context)));
    }

    @Test
    void onlySkillMatchNeedsTheSkillContext() {
        assertTrue(new SkillMatchRanking().needsSkillContext());
        assertFalse(new NewestFirstRanking().needsSkillContext());
        assertFalse(new HighestBudgetRanking().needsSkillContext());
        assertFalse(new DeadlineSoonestRanking().needsSkillContext());
    }

    @Test
    void everyStrategyHandlesAnEmptyListAndLeavesItsInputAlone() {
        List<GigRankingStrategy> all = List.of(new NewestFirstRanking(), new HighestBudgetRanking(),
                new DeadlineSoonestRanking(), new SkillMatchRanking());
        for (GigRankingStrategy strategy : all) {
            assertTrue(strategy.rank(List.of(), RankingContext.empty()).isEmpty());

            List<GigDto> input = new ArrayList<>(List.of(oldCheap, newRich, midSoon));
            strategy.rank(input, RankingContext.empty());
            assertEquals(List.of(1L, 2L, 3L), ids(input), strategy.getClass().getSimpleName());
        }
    }
}
