package com.example.timetracking.timesheet;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import com.example.timetracking.clock.TimeClockEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TimesheetReadServiceTest {
    @Test
    void preservesOriginalsAndAnnotatesOnlyApprovedAdjustments() {
        UUID eventId = UUID.randomUUID();
        UUID adjustmentId = UUID.randomUUID();
        TimeClockEvent event = new TimeClockEvent("tenant-a", "emp-1", eventId, Instant.parse("2026-08-10T12:00:00Z"));
        TimeClockAdjustment adjustment = new TimeClockAdjustment(
                adjustmentId, "tenant-a", "emp-1", eventId, "corrigir omissao", "rh-1",
                Instant.parse("2026-08-11T10:00:00Z"), TimeClockAdjustmentStatus.APPROVED,
                "gestor-1", Instant.parse("2026-08-11T11:00:00Z"));
        CapturingRepository repository = new CapturingRepository(List.of(event), List.of(adjustment));

        List<TimesheetItem> result = new TimesheetReadService(repository).read("tenant-a", "emp-1", YearMonth.of(2026, 8));

        assertThat(result).containsExactly(new TimesheetItem(eventId, event.occurredAt(), TimesheetOrigin.ORIGINAL, List.of(adjustmentId)));
        assertThat(repository.tenantId).isEqualTo("tenant-a");
        assertThat(repository.employeeId).isEqualTo("emp-1");
        assertThat(repository.eventIds).containsExactly(eventId);
    }

    @Test
    void emptyMonthDoesNotQueryAdjustmentsWithFabricatedIds() {
        CapturingRepository repository = new CapturingRepository(List.of(), List.of());

        assertThat(new TimesheetReadService(repository).read("tenant-a", "emp-1", YearMonth.of(2026, 8))).isEmpty();
        assertThat(repository.eventIds).isEmpty();
    }

    private static final class CapturingRepository implements TimesheetReadRepository {
        private final List<TimeClockEvent> events;
        private final List<TimeClockAdjustment> adjustments;
        private String tenantId;
        private String employeeId;
        private Collection<UUID> eventIds = new ArrayList<>();

        private CapturingRepository(List<TimeClockEvent> events, List<TimeClockAdjustment> adjustments) {
            this.events = events;
            this.adjustments = adjustments;
        }

        @Override
        public List<TimeClockEvent> findOriginalEvents(String tenantId, String employeeId, Instant fromInclusive, Instant toExclusive) {
            this.tenantId = tenantId;
            this.employeeId = employeeId;
            return events;
        }

        @Override
        public List<TimeClockAdjustment> findApprovedAdjustments(String tenantId, String employeeId, Collection<UUID> originalClientEventIds) {
            this.eventIds = List.copyOf(originalClientEventIds);
            return adjustments;
        }
    }
}
