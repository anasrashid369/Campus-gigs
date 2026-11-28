package com.campusgigs.service;

import com.campusgigs.dto.RatingSummaryDto;
import com.campusgigs.dto.ReviewDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client reviews of freelancers after a gig is completed (F19). */
public interface ReviewService {

    /** Lowest rating a client can give. */
    int MIN_RATING = 1;

    /** Highest rating a client can give. */
    int MAX_RATING = 5;

    /** Longest allowed comment, in characters. */
    int MAX_COMMENT_LENGTH = 1000;

    /**
     * Saves the client's review of the freelancer who did the gig and announces it with a
     * {@code ReviewPosted} event. Only the client who posted the gig can review it, only after
     * approving the work, and only once.
     *
     * @param comment optional; blank means no comment
     * @throws com.campusgigs.exception.ValidationException if the rating is outside 1-5, the comment is
     *         too long, the caller is not the gig's client, the gig is not approved, or it was already reviewed
     * @throws com.campusgigs.exception.ResourceNotFoundException if the gig or the user does not exist
     */
    ReviewDto postReview(long clientId, long gigId, int rating, String comment);

    /** Every review the freelancer received, newest first. */
    List<ReviewDto> getReviewsForFreelancer(long freelancerId);

    /** The freelancer's average rating; zero reviews when nobody has reviewed them. */
    RatingSummaryDto getRatingSummary(long freelancerId);

    /** The average rating of each given freelancer; every id is in the map, with zero reviews if none. */
    Map<Long, RatingSummaryDto> getRatingSummaries(Collection<Long> freelancerIds);

    /** The ids of the gigs this client has already reviewed. */
    Set<Long> getReviewedGigIds(long clientId);
}
