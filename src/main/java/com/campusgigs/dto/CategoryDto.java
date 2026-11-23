package com.campusgigs.dto;

/** A gig category as the screens see it. Inactive categories cannot be chosen for new gigs. */
public record CategoryDto(long categoryId, String name, boolean active) {
}
