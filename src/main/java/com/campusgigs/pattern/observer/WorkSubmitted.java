package com.campusgigs.pattern.observer;

/** The assigned freelancer submitted the work. The client is the one to notify. */
public record WorkSubmitted(long gigId, long clientId, long freelancerId) implements AppEvent {
}
