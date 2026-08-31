package com.example.timetracking.adjustment;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TimeClockAdjustmentStore {
    TimeClockAdjustment save(TimeClockAdjustment adjustment);

    Optional<TimeClockAdjustment> findByTenantIdAndId(String tenantId, UUID id);

    Optional<TimeClockAdjustment> decideIfPending(
            String tenantId,
            UUID id,
            TimeClockAdjustmentStatus decision,
            String actor,
            Instant decidedAt
    );
}
