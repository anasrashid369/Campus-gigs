package com.campusgigs.pattern.strategy;

import java.util.Map;
import java.util.Set;

/**
 * Facts a ranking may use besides the gigs themselves.
 *
 * @param freelancerSkillIds the skills of the freelancer who searches
 * @param gigSkillIds        the skills each gig asks for, by gig id
 */
public record RankingContext(Set<Long> freelancerSkillIds, Map<Long, Set<Long>> gigSkillIds) {

    public RankingContext {
        freelancerSkillIds = Set.copyOf(freelancerSkillIds);
        gigSkillIds = Map.copyOf(gigSkillIds);
    }

    /** A context with no skill information, for rankings that do not need it. */
    public static RankingContext empty() {
        return new RankingContext(Set.of(), Map.of());
    }
}
