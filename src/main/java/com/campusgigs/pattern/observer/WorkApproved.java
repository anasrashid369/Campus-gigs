package com.campusgigs.pattern.observer;

/** The client approved the work and {@code payoutAmount} tokens (after the fee) went to the freelancer. */
public record WorkApproved(long gigId, long clientId, long freelancerId, long payoutAmount)
        implements AppEvent {
}
