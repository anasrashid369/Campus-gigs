package com.campusgigs.model;

import java.time.LocalDateTime;

/** A freelancer's application to a gig (row of GigApplications). */
public record GigApplication(long applicationId, long gigId, long freelancerId,
                             String proposalMessage, ApplicationStatus status,
                             LocalDateTime submittedAt) {
}
