package com.campusgigs.exception;

/** Thrown when input or a business rule is violated. The message is safe to show to the user. */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
