package com.campusgigs.pattern.observer;

/** A freelancer applied to a client's open gig. The client is the one to notify. */
public record ApplicationSubmitted(long applicationId, long gigId, long clientId, long freelancerId)
        implements AppEvent {
}
