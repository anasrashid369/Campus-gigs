package com.campusgigs.exception;

/**
 * Unchecked exception for any failure in the data access layer
 * (connection problems, SQL errors, failed transactions).
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
