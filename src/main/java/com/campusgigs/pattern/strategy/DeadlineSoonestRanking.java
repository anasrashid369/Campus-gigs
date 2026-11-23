package com.campusgigs.pattern.strategy;

import com.campusgigs.dto.GigDto;

import java.util.Comparator;
import java.util.List;

/** Earliest deadline first; gigs with the same deadline keep the newest-first order. */
public class DeadlineSoonestRanking implements GigRankingStrategy {

    @Override
    public List<GigDto> rank(List<GigDto> gigs, RankingContext context) {
        return gigs.stream()
                .sorted(Comparator.comparing(GigDto::deadline).thenComparing(NewestFirstRanking.NEWEST))
                .toList();
    }
}
