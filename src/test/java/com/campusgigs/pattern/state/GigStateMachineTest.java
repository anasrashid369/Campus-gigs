package com.campusgigs.pattern.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.InvalidGigTransitionException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.GigStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GigStateMachineTest {

    private static final GigStatus OPEN = GigStatus.OPEN;
    private static final GigStatus ASSIGNED = GigStatus.ASSIGNED;
    private static final GigStatus SUBMITTED = GigStatus.SUBMITTED;

    private final GigStateMachine machine = new GigStateMachine();

    /** Every allowed move of the lifecycle. All other (status, action) pairs must be rejected. */
    private static Map<GigStatus, Map<GigAction, GigStatus>> validMoves() {
        Map<GigStatus, Map<GigAction, GigStatus>> moves = new EnumMap<>(GigStatus.class);
        for (GigStatus status : GigStatus.values()) {
            moves.put(status, new EnumMap<>(GigAction.class));
        }
        moves.get(OPEN).put(GigAction.HIRE, ASSIGNED);
        moves.get(OPEN).put(GigAction.CANCEL, GigStatus.CANCELLED);
        moves.get(ASSIGNED).put(GigAction.SUBMIT, SUBMITTED);
        moves.get(SUBMITTED).put(GigAction.APPROVE, GigStatus.APPROVED);
        moves.get(SUBMITTED).put(GigAction.REQUEST_REVISION, ASSIGNED);
        moves.get(SUBMITTED).put(GigAction.RAISE_DISPUTE, GigStatus.DISPUTED);
        moves.get(GigStatus.DISPUTED).put(GigAction.RESOLVE_DISPUTE, GigStatus.RESOLVED);
        return moves;
    }

    static Stream<Arguments> everyStatusAndAction() {
        Map<GigStatus, Map<GigAction, GigStatus>> moves = validMoves();
        return Stream.of(GigStatus.values()).flatMap(status -> Stream.of(GigAction.values())
                .map(action -> Arguments.of(status, action, moves.get(status).get(action))));
    }

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @MethodSource("everyStatusAndAction")
    void followsExactlyTheDocumentedLifecycle(GigStatus status, GigAction action, GigStatus expected) {
        if (expected == null) {
            assertFalse(machine.canTransition(status, action));
            assertThrows(InvalidGigTransitionException.class, () -> machine.transition(status, action));
        } else {
            assertTrue(machine.canTransition(status, action));
            assertEquals(expected, machine.transition(status, action));
        }
    }

    @Test
    void everyStatusHasAState() {
        for (GigStatus status : GigStatus.values()) {
            assertNotNull(machine.allowedActions(status));
        }
    }

    @Test
    void happyPathEndsApproved() {
        GigStatus status = OPEN;
        status = machine.transition(status, GigAction.HIRE);
        status = machine.transition(status, GigAction.SUBMIT);
        status = machine.transition(status, GigAction.APPROVE);

        assertEquals(GigStatus.APPROVED, status);
    }

    @Test
    void revisionReturnsToAssignedAndCanBeResubmitted() {
        GigStatus status = machine.transition(SUBMITTED, GigAction.REQUEST_REVISION);

        assertEquals(ASSIGNED, status);
        assertEquals(SUBMITTED, machine.transition(status, GigAction.SUBMIT));
    }

    @Test
    void disputePathEndsResolved() {
        GigStatus status = machine.transition(SUBMITTED, GigAction.RAISE_DISPUTE);
        status = machine.transition(status, GigAction.RESOLVE_DISPUTE);

        assertEquals(GigStatus.RESOLVED, status);
    }

    @Test
    void onlyAnOpenGigCanBeCancelled() {
        for (GigStatus status : GigStatus.values()) {
            assertEquals(status == OPEN, machine.canTransition(status, GigAction.CANCEL));
        }
    }

    @Test
    void finalStatusesAcceptNothing() {
        Set<GigStatus> finals = EnumSet.of(GigStatus.APPROVED, GigStatus.RESOLVED, GigStatus.CANCELLED);
        for (GigStatus status : GigStatus.values()) {
            assertEquals(finals.contains(status), machine.isFinal(status));
            if (finals.contains(status)) {
                assertTrue(machine.allowedActions(status).isEmpty());
            }
        }
    }

    @Test
    void listsTheActionsAllowedInAStatus() {
        assertEquals(EnumSet.of(GigAction.APPROVE, GigAction.REQUEST_REVISION, GigAction.RAISE_DISPUTE),
                machine.allowedActions(SUBMITTED));
        assertEquals(EnumSet.of(GigAction.HIRE, GigAction.CANCEL), machine.allowedActions(OPEN));
    }

    @Test
    void allowedActionsCannotBeModified() {
        Set<GigAction> actions = machine.allowedActions(OPEN);

        assertThrows(UnsupportedOperationException.class, () -> actions.add(GigAction.APPROVE));
    }

    @Test
    void rejectionMessageIsReadableAndSafeToShow() {
        InvalidGigTransitionException error = assertThrows(InvalidGigTransitionException.class,
                () -> machine.transition(OPEN, GigAction.SUBMIT));

        assertEquals("You cannot submit work on a gig that is open.", error.getMessage());
        assertTrue(error instanceof ValidationException);
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> machine.transition(null, GigAction.HIRE));
        assertThrows(NullPointerException.class, () -> machine.transition(OPEN, null));
    }
}
