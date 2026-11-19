package com.campusgigs.model;

/** A client's rating of a freelancer for a completed gig (row of Reviews). */
public record Review(long reviewId, long gigId, long clientId, long freelancerId,
                     int rating, String comment) {
}
