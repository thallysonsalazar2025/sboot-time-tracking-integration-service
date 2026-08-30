package com.example.timetracking.clock;

import java.time.Instant;
import java.util.UUID;

public record TimeClockSyncResult(
        UUID clientEventId,
        TimeClockSyncStatus status,
        String reason,
        Instant serverReceivedAt
) {
    public static TimeClockSyncResult accepted(UUID clientEventId, TimeClockRegistrationStatus status, Instant serverReceivedAt) {
        return new TimeClockSyncResult(
                clientEventId,
                status == TimeClockRegistrationStatus.CREATED ? TimeClockSyncStatus.CREATED : TimeClockSyncStatus.EXISTING,
                null,
                serverReceivedAt
        );
    }

    public static TimeClockSyncResult accepted(UUID clientEventId, TimeClockRegistrationStatus status) {
        return accepted(clientEventId, status, Instant.now());
    }

    public static TimeClockSyncResult rejected(UUID clientEventId, String reason, Instant serverReceivedAt) {
        return new TimeClockSyncResult(clientEventId, TimeClockSyncStatus.REJECTED, reason, serverReceivedAt);
    }

    public static TimeClockSyncResult rejected(UUID clientEventId, String reason) {
        return rejected(clientEventId, reason, Instant.now());
    }
}
