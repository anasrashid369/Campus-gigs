package com.campusgigs.pattern.observer;

/** A dispute was opened on a gig by {@code raisedBy} (the client or the freelancer). */
public record DisputeRaised(long disputeId, long gigId, long clientId, long freelancerId, long raisedBy)
        implements AppEvent {
}
