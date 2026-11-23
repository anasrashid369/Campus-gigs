package com.campusgigs.pattern.state;

import com.campusgigs.exception.InvalidGigTransitionException;
import com.campusgigs.model.GigStatus;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Entry point of the gig lifecycle (State pattern). Every service asks it before it
 * changes a gig's status, so no status change can skip the rules:
 * <pre>
 * OPEN -> ASSIGNED -> SUBMITTED -> APPROVED
 *   |         ^           |
 *   |         +-----------+  (revision requested)
 *   v                     v
 * CANCELLED            DISPUTED -> RESOLVED
 * </pre>
 * The machine is stateless and thread-safe: the current status always comes from the
 * caller (normally read from the database inside the same transaction).
 */
public class GigStateMachine {

    private final Map<GigStatus, GigState> states = new EnumMap<>(GigStatus.class);

    public GigStateMachine() {
        register(new OpenState());
        register(new AssignedState());
        register(new SubmittedState());
        register(new ApprovedState());
        register(new DisputedState());
        register(new ResolvedState());
        register(new CancelledState());
    }

    private void register(GigState state) {
        states.put(state.status(), state);
    }

    /**
     * Returns the status that follows {@code action}. The caller then saves it.
     *
     * @throws InvalidGigTransitionException if the action is not allowed in the current status
     */
    public GigStatus transition(GigStatus current, GigAction action) {
        Objects.requireNonNull(action, "action");
        return stateOf(current).handle(action);
    }

    /** True if {@link #transition} would accept the action in this status. */
    public boolean canTransition(GigStatus current, GigAction action) {
        Objects.requireNonNull(action, "action");
        return stateOf(current).accepts(action);
    }

    /** The actions accepted in this status; empty for a final status. */
    public Set<GigAction> allowedActions(GigStatus current) {
        return stateOf(current).allowedActions();
    }

    /** True if the gig can no longer change. */
    public boolean isFinal(GigStatus current) {
        return stateOf(current).isFinal();
    }

    private GigState stateOf(GigStatus status) {
        return states.get(Objects.requireNonNull(status, "status"));
    }
}
