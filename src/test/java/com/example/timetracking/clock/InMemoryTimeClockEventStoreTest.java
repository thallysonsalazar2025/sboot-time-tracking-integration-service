package com.example.timetracking.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryTimeClockEventStoreTest {
    private final InMemoryTimeClockEventStore store = new InMemoryTimeClockEventStore();

    @Test
    void repeatedClientEventIsIdempotentInsideSameTenantAndEmployee() {
        UUID clientEventId = UUID.randomUUID();
        TimeClockEvent event = event("tenant-a", "employee-1", clientEventId);

        assertEquals(TimeClockRegistrationStatus.CREATED, store.register(event).status());
        assertEquals(TimeClockRegistrationStatus.EXISTING, store.register(event).status());
    }

    @Test
    void sameClientEventDoesNotCollideAcrossTenants() {
        UUID clientEventId = UUID.randomUUID();

        assertEquals(TimeClockRegistrationStatus.CREATED,
                store.register(event("tenant-a", "employee-1", clientEventId)).status());
        assertEquals(TimeClockRegistrationStatus.CREATED,
                store.register(event("tenant-b", "employee-1", clientEventId)).status());
    }

    @Test
    void sameClientEventDoesNotCollideAcrossEmployees() {
        UUID clientEventId = UUID.randomUUID();

        assertEquals(TimeClockRegistrationStatus.CREATED,
                store.register(event("tenant-a", "employee-1", clientEventId)).status());
        assertEquals(TimeClockRegistrationStatus.CREATED,
                store.register(event("tenant-a", "employee-2", clientEventId)).status());
    }

    @Test
    void rejectsBlankTenant() {
        assertThrows(IllegalArgumentException.class,
                () -> event(" ", "employee-1", UUID.randomUUID()));
    }

    private static TimeClockEvent event(String tenantId, String employeeId, UUID clientEventId) {
        return new TimeClockEvent(tenantId, employeeId, clientEventId, Instant.parse("2026-08-29T18:00:00Z"));
    }
}
