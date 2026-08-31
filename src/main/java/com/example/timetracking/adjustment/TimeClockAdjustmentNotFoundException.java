package com.example.timetracking.adjustment;

public class TimeClockAdjustmentNotFoundException extends RuntimeException {
    public TimeClockAdjustmentNotFoundException() {
        super("adjustment not found");
    }
}
