package com.campusgigs.pattern.observer;

/** A freelancer withdrew their application. The client is the one to notify. */
public record ApplicationWithdrawn(long applicationId, long gigId, long clientId, long freelancerId)
        implements AppEvent {
}
