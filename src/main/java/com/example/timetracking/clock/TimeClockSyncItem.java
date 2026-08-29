package com.example.timetracking.clock;

import java.time.Instant;
import java.util.UUID;

public record TimeClockSyncItem(
        String employeeId,
        UUID clientEventId,
        Instant occurredAt
) {}
