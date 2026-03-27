package com.example.timetracking.exception;

public class ExternalIntegrationException extends RuntimeException {

    public ExternalIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
