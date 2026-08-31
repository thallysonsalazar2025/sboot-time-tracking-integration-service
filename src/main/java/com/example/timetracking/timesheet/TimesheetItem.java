package com.example.timetracking.timesheet;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TimesheetItem(
        UUID clientEventId,
        Instant occurredAt,
        TimesheetOrigin origin,
        List<UUID> approvedAdjustmentIds
) {
    public TimesheetItem {
        approvedAdjustmentIds = List.copyOf(approvedAdjustmentIds);
    }
}
