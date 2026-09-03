package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeClockObservationTest {

    @Test
    void reportsDeviceOffsetWithoutApplyingARejectionThreshold() {
        Instant serverReceivedAt = Instant.parse("2026-09-03T20:00:00Z");
        Instant occurredAt = Instant.parse("2026-09-03T19:58:30Z");

        TimeClockObservation observation = TimeClockObservation.between(occurredAt, serverReceivedAt);

        assertEquals(serverReceivedAt, observation.serverReceivedAt());
        assertEquals(-90L, observation.deviceClockOffsetSeconds());
    }

    @Test
    void preservesFutureOffsetAsTelemetryInsteadOfInventingPolicy() {
        Instant serverReceivedAt = Instant.parse("2026-09-03T20:00:00Z");
        Instant occurredAt = Instant.parse("2026-09-03T20:02:00Z");

        assertEquals(120L, TimeClockObservation.between(occurredAt, serverReceivedAt).deviceClockOffsetSeconds());
    }

    @Test
    void requiresBothInstants() {
        Instant now = Instant.parse("2026-09-03T20:00:00Z");
        assertThrows(NullPointerException.class, () -> TimeClockObservation.between(null, now));
        assertThrows(NullPointerException.class, () -> TimeClockObservation.between(now, null));
    }
}
