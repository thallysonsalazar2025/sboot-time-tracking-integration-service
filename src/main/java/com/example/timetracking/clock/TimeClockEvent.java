package com.example.timetracking.clock;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TimeClockEvent(
        String tenantId,
        String employeeId,
        UUID clientEventId,
        Instant occurredAt
) {
    public TimeClockEvent {
        tenantId = requireText(tenantId, "tenantId");
        employeeId = requireText(employeeId, "employeeId");
        Objects.requireNonNull(clientEventId, "clientEventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
