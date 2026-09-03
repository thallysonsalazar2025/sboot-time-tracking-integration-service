package com.example.timetracking.clock;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Neutral telemetry comparing the device-declared event instant with the trusted
 * server receive instant. This is evidence for audit/observability only: it does
 * not reject a punch or encode a labour-policy tolerance.
 */
public record TimeClockObservation(
        Instant serverReceivedAt,
        long deviceClockOffsetSeconds
) {
    public TimeClockObservation {
        Objects.requireNonNull(serverReceivedAt, "serverReceivedAt");
    }

    public static TimeClockObservation between(Instant occurredAt, Instant serverReceivedAt) {
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(serverReceivedAt, "serverReceivedAt");
        return new TimeClockObservation(
                serverReceivedAt,
                Duration.between(serverReceivedAt, occurredAt).getSeconds()
        );
    }
}
