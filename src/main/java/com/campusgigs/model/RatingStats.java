package com.campusgigs.model;

/** A freelancer's average rating and number of reviews (Reviews grouped by freelancer). */
public record RatingStats(long freelancerId, double averageRating, int reviewCount) {
}
