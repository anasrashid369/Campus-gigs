package com.campusgigs.payment;

import java.util.UUID;

/** Simulated gateway: approves every request with a positive amount. No real money is moved. */
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult process(PaymentRequest request) {
        if (request == null) {
            return PaymentResult.failed("Payment request is missing");
        }
        if (request.amount() <= 0) {
            return PaymentResult.failed("Amount must be greater than zero");
        }
        return PaymentResult.succeeded("SIM-" + UUID.randomUUID());
    }
}
