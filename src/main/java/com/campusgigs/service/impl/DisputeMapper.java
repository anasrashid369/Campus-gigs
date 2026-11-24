package com.campusgigs.service.impl;

import com.campusgigs.dto.DisputeDto;
import com.campusgigs.model.Dispute;
import com.campusgigs.model.Gig;

/** Builds a {@link DisputeDto} from a dispute and its gig. */
final class DisputeMapper {

    private DisputeMapper() {
    }

    static DisputeDto toDto(Dispute dispute, Gig gig) {
        return new DisputeDto(dispute.disputeId(), gig.gigId(), gig.title(), gig.clientId(),
                gig.assignedFreelancerId() == null ? 0 : gig.assignedFreelancerId(), gig.budget(),
                dispute.raisedBy(), dispute.reason(), dispute.status(), dispute.decision(),
                dispute.resolvedBy(), dispute.resolvedAt());
    }
}
