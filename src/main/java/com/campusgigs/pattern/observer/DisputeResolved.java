package com.campusgigs.pattern.observer;

import com.campusgigs.model.DisputeDecision;

import java.util.Objects;

/** An admin resolved a dispute by releasing the escrow to the freelancer or refunding the client. */
public record DisputeResolved(long disputeId, long gigId, long clientId, long freelancerId,
                              DisputeDecision decision) implements AppEvent {

    public DisputeResolved {
        Objects.requireNonNull(decision, "decision");
    }
}
