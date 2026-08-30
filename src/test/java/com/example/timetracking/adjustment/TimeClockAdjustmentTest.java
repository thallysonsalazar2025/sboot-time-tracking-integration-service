package com.example.timetracking.adjustment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimeClockAdjustmentTest {
    private static final Instant REQUESTED_AT = Instant.parse("2026-08-30T10:00:00Z");
    private static final Instant DECIDED_AT = Instant.parse("2026-08-30T10:05:00Z");

    @Test
    void createsPendingAdjustmentWithoutMutatingOriginalIdentity() {
        UUID originalClientEventId = UUID.randomUUID();
        TimeClockAdjustment adjustment = TimeClockAdjustment.request(
                "tenant-a", "employee-1", originalClientEventId,
                "Corrigir omissão registrada no espelho", "employee-1", REQUESTED_AT);

        assertEquals(TimeClockAdjustmentStatus.PENDING_APPROVAL, adjustment.status());
        assertEquals(originalClientEventId, adjustment.originalClientEventId());
        assertEquals("tenant-a", adjustment.tenantId());
    }

    @Test
    void approvesWithDecisionAuditMetadata() {
        TimeClockAdjustment approved = adjustment().decide(
                TimeClockAdjustmentStatus.APPROVED, "manager-1", DECIDED_AT);

        assertEquals(TimeClockAdjustmentStatus.APPROVED, approved.status());
        assertEquals("manager-1", approved.decidedBy());
        assertEquals(DECIDED_AT, approved.decidedAt());
    }

    @Test
    void preventsContradictorySecondDecision() {
        TimeClockAdjustment approved = adjustment().decide(
                TimeClockAdjustmentStatus.APPROVED, "manager-1", DECIDED_AT);

        assertThrows(IllegalStateException.class, () -> approved.decide(
                TimeClockAdjustmentStatus.REJECTED, "manager-2", DECIDED_AT.plusSeconds(30)));
    }

    @Test
    void requiresNonBlankJustification() {
        assertThrows(IllegalArgumentException.class, () -> TimeClockAdjustment.request(
                "tenant-a", "employee-1", UUID.randomUUID(), " ", "employee-1", REQUESTED_AT));
    }

    private static TimeClockAdjustment adjustment() {
        return TimeClockAdjustment.request(
                "tenant-a", "employee-1", UUID.randomUUID(),
                "Ajuste auditável", "employee-1", REQUESTED_AT);
    }
}
