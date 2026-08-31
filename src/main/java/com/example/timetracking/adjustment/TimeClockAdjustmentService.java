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
                .orElseThrow(TimeClockAdjustmentNotFoundException::new);

        if (current.status() != TimeClockAdjustmentStatus.PENDING_APPROVAL) {
            return resolveTerminalReplay(current, decision);
        }

        Instant decidedAt = clock.instant();
        current.decide(decision, actor, decidedAt);

        return store.decideIfPending(tenantId, adjustmentId, decision, actor, decidedAt)
                .orElseGet(() -> {
                    TimeClockAdjustment committed = store.findByTenantIdAndId(tenantId, adjustmentId)
                            .orElseThrow(TimeClockAdjustmentNotFoundException::new);
                    return resolveTerminalReplay(committed, decision);
                });
    }

    private TimeClockAdjustment resolveTerminalReplay(
            TimeClockAdjustment current,
            TimeClockAdjustmentStatus requestedDecision
    ) {
        if (current.status() == requestedDecision) {
            return current;
        }
        throw new TimeClockAdjustmentDecisionConflictException();
    }
}
