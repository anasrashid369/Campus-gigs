package com.campusgigs.pattern.strategy;

import com.campusgigs.dto.GigDto;

import java.util.Comparator;
import java.util.List;

/** Newest gigs first. */
public class NewestFirstRanking implements GigRankingStrategy {

    static final Comparator<GigDto> NEWEST = Comparator.comparing(GigDto::createdAt).reversed()
            .thenComparing(Comparator.comparingLong(GigDto::gigId).reversed());

    @Override
    public List<GigDto> rank(List<GigDto> gigs, RankingContext context) {
        return gigs.stream().sorted(NEWEST).toList();
    }
}
