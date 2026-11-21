package com.campusgigs.pattern.observer;

/** The client reviewed the freelancer of a completed gig. The freelancer is the one to notify. */
public record ReviewPosted(long reviewId, long gigId, long clientId, long freelancerId, int rating)
        implements AppEvent {
}
