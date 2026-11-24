package com.campusgigs.pattern.strategy;

import com.campusgigs.dto.GigDto;

import java.util.List;

/**
 * Strategy pattern: one way to order the open gigs a freelancer sees. A new ranking is a new
 * class registered with the search service; the service itself does not change.
 */
public interface GigRankingStrategy {

    /** Returns the gigs in the strategy's order. The given list is never changed. */
    List<GigDto> rank(List<GigDto> gigs, RankingContext context);

    /** True if {@link #rank} needs the skill information of the freelancer and the gigs. */
    default boolean needsSkillContext() {
        return false;
    }
}
