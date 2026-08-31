package com.example.timetracking.adjustment;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class TimeClockAdjustmentService {
    private final TimeClockAdjustmentStore store;
    private final Clock clock;

    public TimeClockAdjustmentService(TimeClockAdjustmentStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public TimeClockAdjustment decide(
            String tenantId,
            UUID adjustmentId,
            TimeClockAdjustmentStatus decision,
            String actor
    ) {
        TimeClockAdjustment current = store.findByTenantIdAndId(tenantId, adjustmentId)
                .orElseThrow(() -> new IllegalArgumentException("adjustment not found"));

        if (current.status() != TimeClockAdjustmentStatus.PENDING_APPROVAL) {
            if (current.status() == decision) {
                return current;
            }
            throw new IllegalStateException("adjustment already decided");
        }

        Instant decidedAt = clock.instant();
        return store.save(current.decide(decision, actor, decidedAt));
    }
}
