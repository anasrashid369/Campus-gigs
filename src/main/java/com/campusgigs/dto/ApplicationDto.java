package com.campusgigs.dto;

import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;

import java.time.LocalDateTime;

/** An application together with the gig and freelancer details the screens show. */
public record ApplicationDto(long applicationId, long gigId, String gigTitle, GigStatus gigStatus,
                             long freelancerId, String freelancerName, String proposalMessage,
                             ApplicationStatus status, LocalDateTime submittedAt) {
}
