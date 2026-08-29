package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeClockBatchSyncServiceTest {

    @Test
    void returnsCreatedExistingAndRejectedPerItemWithoutTrustingTenantFromPayload() {
        TimeClockBatchSyncService service = new TimeClockBatchSyncService(new InMemoryTimeClockEventStore());
        UUID repeated = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-29T20:00:00Z");

        List<TimeClockSyncResult> results = service.sync("tenant-a", List.of(
                new TimeClockSyncItem("employee-1", repeated, occurredAt),
                new TimeClockSyncItem("employee-1", repeated, occurredAt),
                new TimeClockSyncItem(" ", UUID.randomUUID(), occurredAt)
        ));

        assertEquals(TimeClockSyncStatus.CREATED, results.get(0).status());
        assertEquals(TimeClockSyncStatus.EXISTING, results.get(1).status());
        assertEquals(TimeClockSyncStatus.REJECTED, results.get(2).status());
        assertEquals("INVALID_EVENT", results.get(2).reason());
    }

    @Test
    void sameClientEventIdCanExistInDifferentTrustedTenants() {
        TimeClockBatchSyncService service = new TimeClockBatchSyncService(new InMemoryTimeClockEventStore());
        UUID clientEventId = UUID.randomUUID();
        TimeClockSyncItem item = new TimeClockSyncItem(
                "employee-1",
                clientEventId,
                Instant.parse("2026-08-29T20:00:00Z")
        );

        assertEquals(TimeClockSyncStatus.CREATED, service.sync("tenant-a", List.of(item)).get(0).status());
        assertEquals(TimeClockSyncStatus.CREATED, service.sync("tenant-b", List.of(item)).get(0).status());
    }

    @Test
    void rejectsMissingTrustedTenantBeforeAnyWrite() {
        TimeClockBatchSyncService service = new TimeClockBatchSyncService(new InMemoryTimeClockEventStore());
        assertThrows(IllegalArgumentException.class, () -> service.sync(" ", List.of()));
    }
}
