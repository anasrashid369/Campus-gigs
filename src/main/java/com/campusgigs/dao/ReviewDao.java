package com.campusgigs.dao;

import com.campusgigs.model.RatingStats;
import com.campusgigs.model.Review;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Data access for client reviews of freelancers (table Reviews). */
public interface ReviewDao {

    /**
     * Saves a review and returns its id. The table allows one review per gig.
     *
     * @param comment the client's comment, or null for none
     */
    long insert(long gigId, long clientId, long freelancerId, int rating, String comment);

    /** The review of the gig, if it has one. */
    Optional<Review> findByGigId(long gigId);

    /** Every review the freelancer received, newest first. */
    List<Review> findByFreelancerId(long freelancerId);

    /** The ids of the gigs the client has already reviewed. */
    List<Long> findGigIdsReviewedByClient(long clientId);

    /** Average rating and review count of each given freelancer who has at least one review. */
    List<RatingStats> findRatingStats(Collection<Long> freelancerIds);
}
