package com.campusgigs.payment;

/** A request to charge tokens to the user's simulated payment source. */
public record PaymentRequest(long userId, long amount, String description) {
}
