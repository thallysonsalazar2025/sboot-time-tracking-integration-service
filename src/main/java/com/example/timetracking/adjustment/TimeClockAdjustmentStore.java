package com.example.timetracking.adjustment;

import java.util.Optional;
import java.util.UUID;

public interface TimeClockAdjustmentStore {
    TimeClockAdjustment save(TimeClockAdjustment adjustment);

    Optional<TimeClockAdjustment> findByTenantIdAndId(String tenantId, UUID id);
}
