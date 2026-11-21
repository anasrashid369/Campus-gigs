package com.campusgigs.pattern.observer;

/** The client sent the work back for changes. The freelancer is the one to notify. */
public record RevisionRequested(long gigId, long clientId, long freelancerId) implements AppEvent {
}
