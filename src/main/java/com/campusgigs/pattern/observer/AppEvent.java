package com.campusgigs.pattern.observer;

/**
 * Base type of every event that travels through the {@link EventBus}.
 * <p>
 * Each event is an immutable record that only carries facts (ids and amounts).
 * Deciding who is notified and what the message says belongs to the listeners,
 * not to the service that publishes the event. The permitted list is the set of
 * event names agreed by the team; do not add or rename one without telling the group.
 */
public sealed interface AppEvent permits
        ApplicationSubmitted, ApplicationWithdrawn,
        FreelancerHired, WorkSubmitted, WorkApproved, RevisionRequested,
        DisputeRaised, DisputeResolved,
        WalletToppedUp, ReviewPosted {
}
