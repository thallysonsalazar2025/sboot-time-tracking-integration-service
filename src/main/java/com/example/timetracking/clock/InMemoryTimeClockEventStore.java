package com.example.timetracking.clock;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class InMemoryTimeClockEventStore implements TimeClockEventStore {
    private final Map<EventKey, TimeClockEvent> events = new HashMap<>();

    @Override
    public synchronized TimeClockRegistrationResult register(TimeClockEvent event) {
        Objects.requireNonNull(event, "event");
        EventKey key = new EventKey(event.tenantId(), event.employeeId(), event.clientEventId());
        TimeClockEvent existing = events.get(key);
        if (existing != null) {
            return new TimeClockRegistrationResult(TimeClockRegistrationStatus.EXISTING, existing);
        }
        events.put(key, event);
        return new TimeClockRegistrationResult(TimeClockRegistrationStatus.CREATED, event);
    }

    private record EventKey(String tenantId, String employeeId, UUID clientEventId) {}
}
