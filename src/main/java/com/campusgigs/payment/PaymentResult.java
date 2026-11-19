package com.campusgigs.payment;

/**
 * Outcome of a payment attempt. {@code reference} identifies the payment when
 * successful; {@code message} explains a failure.
 */
public record PaymentResult(boolean success, String reference, String message) {

    public static PaymentResult succeeded(String reference) {
        return new PaymentResult(true, reference, "Payment approved");
    }

    public static PaymentResult failed(String message) {
        return new PaymentResult(false, null, message);
    }
}
