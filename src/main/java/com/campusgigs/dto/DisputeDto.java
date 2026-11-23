package com.campusgigs.dto;

import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;

import java.time.LocalDateTime;

/**
 * A dispute together with the facts an administrator needs to decide it.
 * {@code decision}, {@code resolvedBy} and {@code resolvedAt} are null while it is open.
 */
public record DisputeDto(long disputeId, long gigId, String gigTitle, long clientId, long freelancerId,
                         long budget, long raisedBy, String reason, DisputeStatus status,
                         DisputeDecision decision, Long resolvedBy, LocalDateTime resolvedAt) {
}
