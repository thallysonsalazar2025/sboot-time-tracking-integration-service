package com.example.timetracking.adjustment;

public class TimeClockAdjustmentDecisionConflictException extends RuntimeException {
    public TimeClockAdjustmentDecisionConflictException() {
        super("adjustment already decided");
    }
}
