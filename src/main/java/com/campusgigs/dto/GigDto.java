package com.campusgigs.dto;

import com.campusgigs.model.GigStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A gig as the screens see it. {@code assignedFreelancerId} is null until a
 * freelancer is hired. The budget is in whole tokens.
 */
public record GigDto(long gigId, long clientId, long categoryId, Long assignedFreelancerId,
                     String title, String description, long budget, LocalDate deadline,
                     GigStatus status, LocalDateTime createdAt) {
}
