package com.campusgigs.pattern.observer;

/** The client hired a freelancer and {@code escrowAmount} tokens were moved into escrow. */
public record FreelancerHired(long gigId, long clientId, long freelancerId, long escrowAmount)
        implements AppEvent {
}
