package com.example.timetracking.clock;

import java.util.UUID;

public record TimeClockSyncResult(
        UUID clientEventId,
        TimeClockSyncStatus status,
        String reason
) {
    public static TimeClockSyncResult accepted(UUID clientEventId, TimeClockRegistrationStatus status) {
        return new TimeClockSyncResult(
                clientEventId,
                status == TimeClockRegistrationStatus.CREATED ? TimeClockSyncStatus.CREATED : TimeClockSyncStatus.EXISTING,
                null
        );
    }

    public static TimeClockSyncResult rejected(UUID clientEventId, String reason) {
        return new TimeClockSyncResult(clientEventId, TimeClockSyncStatus.REJECTED, reason);
    }
}
