package com.campusgigs.pattern.strategy;

import com.campusgigs.dto.GigDto;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Gigs that ask for more of the freelancer's skills first; ties keep the newest-first order. */
public class SkillMatchRanking implements GigRankingStrategy {

    @Override
    public List<GigDto> rank(List<GigDto> gigs, RankingContext context) {
        Comparator<GigDto> byMatches = Comparator.comparingLong((GigDto gig) -> matches(gig, context)).reversed();
        return gigs.stream().sorted(byMatches.thenComparing(NewestFirstRanking.NEWEST)).toList();
    }

    @Override
    public boolean needsSkillContext() {
        return true;
    }

    private static long matches(GigDto gig, RankingContext context) {
        Set<Long> asked = context.gigSkillIds().getOrDefault(gig.gigId(), Set.of());
        return asked.stream().filter(context.freelancerSkillIds()::contains).count();
    }
}
