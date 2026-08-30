package com.example.timetracking.adjustment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TimeClockAdjustment(
        UUID id,
        String tenantId,
        String employeeId,
        UUID originalClientEventId,
        String justification,
        String requestedBy,
        Instant requestedAt,
        TimeClockAdjustmentStatus status,
        String decidedBy,
        Instant decidedAt
) {
    public TimeClockAdjustment {
        Objects.requireNonNull(id, "id");
        tenantId = requireText(tenantId, "tenantId");
        employeeId = requireText(employeeId, "employeeId");
        Objects.requireNonNull(originalClientEventId, "originalClientEventId");
        justification = requireText(justification, "justification");
        requestedBy = requireText(requestedBy, "requestedBy");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(status, "status");
        if (status == TimeClockAdjustmentStatus.PENDING_APPROVAL && (decidedBy != null || decidedAt != null)) {
            throw new IllegalArgumentException("pending adjustment cannot have decision metadata");
        }
        if (status != TimeClockAdjustmentStatus.PENDING_APPROVAL) {
            decidedBy = requireText(decidedBy, "decidedBy");
            Objects.requireNonNull(decidedAt, "decidedAt");
        }
    }

    public static TimeClockAdjustment request(
            String tenantId,
            String employeeId,
            UUID originalClientEventId,
            String justification,
            String requestedBy,
            Instant requestedAt
    ) {
        return new TimeClockAdjustment(
                UUID.randomUUID(), tenantId, employeeId, originalClientEventId, justification,
                requestedBy, requestedAt, TimeClockAdjustmentStatus.PENDING_APPROVAL, null, null);
    }

    public TimeClockAdjustment decide(TimeClockAdjustmentStatus decision, String actor, Instant at) {
        if (status != TimeClockAdjustmentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("adjustment already decided");
        }
        if (decision != TimeClockAdjustmentStatus.APPROVED
                && decision != TimeClockAdjustmentStatus.REJECTED
                && decision != TimeClockAdjustmentStatus.CANCELLED) {
            throw new IllegalArgumentException("invalid terminal decision");
        }
        return new TimeClockAdjustment(id, tenantId, employeeId, originalClientEventId, justification,
                requestedBy, requestedAt, decision, actor, at);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
