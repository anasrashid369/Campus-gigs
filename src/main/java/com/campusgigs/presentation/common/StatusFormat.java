package com.campusgigs.presentation.common;

import com.campusgigs.dto.GigDto;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;

import java.time.LocalDate;

/** Turns gig statuses into the words shown on screen and marks late work. */
public final class StatusFormat {

    private StatusFormat() {
    }

    /** A readable name for a gig status; ASSIGNED reads "In progress". */
    public static String gigStatus(GigStatus status) {
        return switch (status) {
            case OPEN -> "Open";
            case ASSIGNED -> "In progress";
            case SUBMITTED -> "Submitted";
            case APPROVED -> "Approved";
            case DISPUTED -> "Disputed";
            case RESOLVED -> "Resolved";
            case CANCELLED -> "Cancelled";
        };
    }

    /** A readable name for an application status. */
    public static String applicationStatus(ApplicationStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Not chosen";
            case WITHDRAWN -> "Withdrawn";
        };
    }

    /** True when the freelancer is still working on the gig and its deadline has passed. */
    public static boolean isOverdue(GigDto gig, LocalDate today) {
        return gig.status() == GigStatus.ASSIGNED && gig.deadline().isBefore(today);
    }
}
