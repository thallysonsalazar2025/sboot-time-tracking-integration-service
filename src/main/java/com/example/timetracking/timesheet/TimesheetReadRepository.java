package com.example.timetracking.timesheet;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.clock.TimeClockEvent;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TimesheetReadRepository {
    List<TimeClockEvent> findOriginalEvents(String tenantId, String employeeId, Instant fromInclusive, Instant toExclusive);

    List<TimeClockAdjustment> findApprovedAdjustments(
            String tenantId,
            String employeeId,
            Collection<UUID> originalClientEventIds
    );
}
