package com.example.timetracking.timesheet;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.clock.TimeClockEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TimesheetReadService {
    private final TimesheetReadRepository repository;

    public TimesheetReadService(TimesheetReadRepository repository) {
        this.repository = repository;
    }

    public List<TimesheetItem> read(String tenantId, String employeeId, YearMonth competence, ZoneId businessZone) {
        requireText(tenantId, "tenantId");
        requireText(employeeId, "employeeId");
        if (competence == null) {
            throw new IllegalArgumentException("competence is required");
        }
        if (businessZone == null) {
            throw new IllegalArgumentException("businessZone is required");
        }

        Instant fromInclusive = competence.atDay(1).atStartOfDay(businessZone).toInstant();
        Instant toExclusive = competence.plusMonths(1).atDay(1).atStartOfDay(businessZone).toInstant();

        List<TimeClockEvent> originals = repository.findOriginalEvents(tenantId, employeeId, fromInclusive, toExclusive);
        List<UUID> eventIds = originals.stream().map(TimeClockEvent::clientEventId).toList();
        Map<UUID, List<UUID>> approvedByOriginal = repository.findApprovedAdjustments(tenantId, employeeId, eventIds)
                .stream()
                .collect(Collectors.groupingBy(
                        TimeClockAdjustment::originalClientEventId,
                        Collectors.mapping(TimeClockAdjustment::id, Collectors.toList())
                ));

        return originals.stream()
                .map(event -> new TimesheetItem(
                        event.clientEventId(),
                        event.occurredAt(),
                        TimesheetOrigin.ORIGINAL,
                        approvedByOriginal.getOrDefault(event.clientEventId(), List.of())
                ))
                .toList();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
