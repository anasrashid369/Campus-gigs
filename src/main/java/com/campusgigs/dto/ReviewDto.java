package com.campusgigs.dto;

/**
 * A review as the screens show it, with the gig title and the client's name.
 *
 * @param comment the client's comment, or null when they left none
 */
public record ReviewDto(long reviewId, long gigId, String gigTitle, String clientName, long freelancerId,
                        int rating, String comment) {
}
