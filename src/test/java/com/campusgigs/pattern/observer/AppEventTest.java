package com.campusgigs.pattern.observer;

import com.campusgigs.model.DisputeDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppEventTest {

    @Test
    void hireEventKeepsItsValues() {
        FreelancerHired event = new FreelancerHired(7, 3, 4, 250);

        assertEquals(7, event.gigId());
        assertEquals(3, event.clientId());
        assertEquals(4, event.freelancerId());
        assertEquals(250, event.escrowAmount());
    }

    @Test
    void eventsWithSameValuesAreEqual() {
        assertEquals(new WorkSubmitted(7, 3, 4), new WorkSubmitted(7, 3, 4));
    }

    @Test
    void disputeResolvedKeepsDecision() {
        DisputeResolved event = new DisputeResolved(1, 7, 3, 4, DisputeDecision.RELEASE);

        assertEquals(DisputeDecision.RELEASE, event.decision());
    }

    @Test
    void disputeResolvedRequiresDecision() {
        assertThrows(NullPointerException.class, () -> new DisputeResolved(1, 7, 3, 4, null));
    }
}
