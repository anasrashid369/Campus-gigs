package com.campusgigs.dto;

/**
 * A freelancer's average rating (1 to 5) over all their reviews.
 *
 * @param averageRating 0 when the freelancer has no reviews yet
 */
public record RatingSummaryDto(long freelancerId, double averageRating, int reviewCount) {

    /** A freelancer nobody has reviewed yet. */
    public static RatingSummaryDto none(long freelancerId) {
        return new RatingSummaryDto(freelancerId, 0, 0);
    }

    /** True when at least one review exists. */
    public boolean hasReviews() {
        return reviewCount > 0;
    }
}
