package com.campusgigs.pattern.strategy;

import com.campusgigs.dto.GigDto;

import java.util.Comparator;
import java.util.List;

/** Highest budget first; gigs with the same budget keep the newest-first order. */
public class HighestBudgetRanking implements GigRankingStrategy {

    @Override
    public List<GigDto> rank(List<GigDto> gigs, RankingContext context) {
        return gigs.stream()
                .sorted(Comparator.comparingLong(GigDto::budget).reversed().thenComparing(NewestFirstRanking.NEWEST))
                .toList();
    }
}
