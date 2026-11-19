package com.campusgigs.payment;

/**
 * Boundary to the payment provider. Only wallet top-up uses it; escrow and
 * internal transfers never go through the gateway.
 */
public interface PaymentGateway {

    /** Processes the payment and reports the outcome; never throws for a declined payment. */
    PaymentResult process(PaymentRequest request);
}
