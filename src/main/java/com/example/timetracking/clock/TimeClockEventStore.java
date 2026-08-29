package com.example.timetracking.clock;

public interface TimeClockEventStore {
    TimeClockRegistrationResult register(TimeClockEvent event);
}
