package com.campusgigs.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A gig (row of Gigs). The budget is in whole tokens.
 * {@code assignedFreelancerId} is null until a freelancer is hired.
 */
public record Gig(long gigId, long clientId, long categoryId, Long assignedFreelancerId,
                  String title, String description, long budget, LocalDate deadline,
                  GigStatus status, LocalDateTime createdAt) {
}
