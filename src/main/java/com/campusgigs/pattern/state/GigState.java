package com.campusgigs.pattern.state;

import com.campusgigs.exception.InvalidGigTransitionException;
import com.campusgigs.model.GigStatus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * State pattern: one subclass per gig status. A state knows which actions it accepts
 * and which status each action leads to; anything else is rejected.
 * States hold no per-gig data, so one shared instance per status is enough.
 */
abstract class GigState {

    private final GigStatus status;
    private final Map<GigAction, GigStatus> transitions = new EnumMap<>(GigAction.class);

    protected GigState(GigStatus status) {
        this.status = status;
    }

    /** Declares that {@code action} moves a gig from this state to {@code next}. */
    protected final void allow(GigAction action, GigStatus next) {
        transitions.put(action, next);
    }

    final GigStatus status() {
        return status;
    }

    final boolean accepts(GigAction action) {
        return transitions.containsKey(action);
    }

    final Set<GigAction> allowedActions() {
        return Collections.unmodifiableSet(transitions.keySet());
    }

    /** True when no action is accepted any more. */
    final boolean isFinal() {
        return transitions.isEmpty();
    }

    /**
     * Applies the action and returns the status that follows.
     *
     * @throws InvalidGigTransitionException if this state does not accept the action
     */
    final GigStatus handle(GigAction action) {
        GigStatus next = transitions.get(action);
        if (next == null) {
            throw new InvalidGigTransitionException("You cannot " + action.description()
                    + " a gig that is " + status.name().toLowerCase(Locale.ROOT) + ".");
        }
        return next;
    }
}
