package com.example.timetracking.adjustment;

public interface TimeClockAdjustmentEventPublisher {
    void publishApproved(TimeClockAdjustment adjustment);

    static TimeClockAdjustmentEventPublisher noop() {
        return adjustment -> { };
    }
}
