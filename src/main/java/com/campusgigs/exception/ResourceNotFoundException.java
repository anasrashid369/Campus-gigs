package com.campusgigs.exception;

/** Thrown when a requested record does not exist. The message is safe to show to the user. */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
