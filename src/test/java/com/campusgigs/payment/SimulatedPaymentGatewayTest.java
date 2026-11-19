package com.campusgigs.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway = new SimulatedPaymentGateway();

    @Test
    void approvesPositiveAmount() {
        PaymentResult result = gateway.process(new PaymentRequest(1, 500, "top-up"));

        assertTrue(result.success());
        assertTrue(result.reference().startsWith("SIM-"));
    }

    @Test
    void referencesAreUnique() {
        PaymentResult a = gateway.process(new PaymentRequest(1, 5, "top-up"));
        PaymentResult b = gateway.process(new PaymentRequest(1, 5, "top-up"));

        assertNotEquals(a.reference(), b.reference());
    }

    @Test
    void declinesZeroAndNegativeAmounts() {
        assertFalse(gateway.process(new PaymentRequest(1, 0, "x")).success());
        assertFalse(gateway.process(new PaymentRequest(1, -5, "x")).success());
    }

    @Test
    void declinesMissingRequest() {
        PaymentResult result = gateway.process(null);

        assertFalse(result.success());
        assertEquals("Payment request is missing", result.message());
    }
}
