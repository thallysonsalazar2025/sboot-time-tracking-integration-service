package com.example.timetracking.clock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TimeClockBatchSyncService {
    private final TimeClockEventStore store;

    public TimeClockBatchSyncService(TimeClockEventStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public List<TimeClockSyncResult> sync(String trustedTenantId, List<TimeClockSyncItem> items) {
        if (trustedTenantId == null || trustedTenantId.isBlank()) {
            throw new IllegalArgumentException("trustedTenantId must not be blank");
        }
        Objects.requireNonNull(items, "items");

        List<TimeClockSyncResult> results = new ArrayList<>(items.size());
        for (TimeClockSyncItem item : items) {
            if (item == null) {
                results.add(TimeClockSyncResult.rejected(null, "INVALID_EVENT"));
                continue;
            }
            try {
                TimeClockEvent event = new TimeClockEvent(
                        trustedTenantId,
                        item.employeeId(),
                        item.clientEventId(),
                        item.occurredAt()
                );
                TimeClockRegistrationResult registration = store.register(event);
                results.add(TimeClockSyncResult.accepted(item.clientEventId(), registration.status()));
            } catch (IllegalArgumentException | NullPointerException invalidEvent) {
                results.add(TimeClockSyncResult.rejected(item.clientEventId(), "INVALID_EVENT"));
            }
        }
        return List.copyOf(results);
    }
}
