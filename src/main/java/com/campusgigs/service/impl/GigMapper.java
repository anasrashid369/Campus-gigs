package com.campusgigs.service.impl;

import com.campusgigs.dto.GigDto;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;

/** Turns a {@link Gig} into a {@link GigDto}. */
final class GigMapper {

    private GigMapper() {
    }

    static GigDto toDto(Gig gig) {
        return toDto(gig, gig.status(), gig.assignedFreelancerId());
    }

    /** The gig as it is after a change that has just been saved. */
    static GigDto toDto(Gig gig, GigStatus status, Long assignedFreelancerId) {
        return new GigDto(gig.gigId(), gig.clientId(), gig.categoryId(), assignedFreelancerId,
                gig.title(), gig.description(), gig.budget(), gig.deadline(), status, gig.createdAt());
    }
}
