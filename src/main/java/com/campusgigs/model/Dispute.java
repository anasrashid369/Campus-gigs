package com.campusgigs.model;

import java.time.LocalDateTime;

/**
 * A dispute on a gig (row of Disputes). {@code resolvedBy}, {@code decision}
 * and {@code resolvedAt} are null while the dispute is open.
 */
public record Dispute(long disputeId, long gigId, long raisedBy, Long resolvedBy, String reason,
                      DisputeStatus status, DisputeDecision decision, LocalDateTime resolvedAt) {
}
