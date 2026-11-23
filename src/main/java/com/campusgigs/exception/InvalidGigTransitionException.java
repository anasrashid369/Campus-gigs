package com.campusgigs.exception;

/**
 * Thrown when an action is not allowed in the gig's current status, for example
 * submitting work on a gig that is still open. The message is safe to show to the user.
 */
public class InvalidGigTransitionException extends ValidationException {

    private static final long serialVersionUID = 1L;

    public InvalidGigTransitionException(String message) {
        super(message);
    }
}
