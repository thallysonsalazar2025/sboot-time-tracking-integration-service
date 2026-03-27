package com.example.timetracking.exception;

import java.util.UUID;

public class IntegrationConfigNotFoundException extends RuntimeException {

    public IntegrationConfigNotFoundException(UUID companyId) {
        super("Active integration configuration not found for companyId=" + companyId);
    }
}
