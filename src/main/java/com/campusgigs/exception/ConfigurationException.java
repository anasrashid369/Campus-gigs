package com.campusgigs.exception;

/**
 * Thrown when the application configuration is missing or incomplete.
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
