package com.example.timetracking.timesheet;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import com.example.timetracking.clock.TimeClockEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimesheetReadServiceTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

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

        List<TimesheetItem> result = new TimesheetReadService(repository)
                .read("tenant-a", "emp-1", YearMonth.of(2026, 8), SAO_PAULO);

        assertThat(result).containsExactly(new TimesheetItem(eventId, event.occurredAt(), TimesheetOrigin.ORIGINAL, List.of(adjustmentId)));
        assertThat(repository.tenantId).isEqualTo("tenant-a");
        assertThat(repository.employeeId).isEqualTo("emp-1");
        assertThat(repository.eventIds).containsExactly(eventId);
    }

    @Test
    void usesBusinessTimezoneForCompetenceBoundaries() {
        CapturingRepository repository = new CapturingRepository(List.of(), List.of());

        new TimesheetReadService(repository).read("tenant-a", "emp-1", YearMonth.of(2026, 8), SAO_PAULO);

        assertThat(repository.fromInclusive).isEqualTo(Instant.parse("2026-08-01T03:00:00Z"));
        assertThat(repository.toExclusive).isEqualTo(Instant.parse("2026-09-01T03:00:00Z"));
    }

    @Test
    void emptyMonthDoesNotQueryAdjustmentsWithFabricatedIds() {
        CapturingRepository repository = new CapturingRepository(List.of(), List.of());

        assertThat(new TimesheetReadService(repository)
                .read("tenant-a", "emp-1", YearMonth.of(2026, 8), SAO_PAULO)).isEmpty();
        assertThat(repository.eventIds).isEmpty();
    }

    @Test
    void rejectsMissingReadContextBeforeRepositoryAccess() {
        CapturingRepository repository = new CapturingRepository(List.of(), List.of());
        TimesheetReadService service = new TimesheetReadService(repository);

        assertThatThrownBy(() -> service.read(" ", "emp-1", YearMonth.of(2026, 8), SAO_PAULO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId is required");
        assertThatThrownBy(() -> service.read("tenant-a", " ", YearMonth.of(2026, 8), SAO_PAULO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("employeeId is required");
        assertThatThrownBy(() -> service.read("tenant-a", "emp-1", null, SAO_PAULO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("competence is required");
        assertThatThrownBy(() -> service.read("tenant-a", "emp-1", YearMonth.of(2026, 8), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("businessZone is required");

        assertThat(repository.tenantId).isNull();
        assertThat(repository.employeeId).isNull();
        assertThat(repository.eventIds).isEmpty();
    }

    private static final class CapturingRepository implements TimesheetReadRepository {
        private final List<TimeClockEvent> events;
        private final List<TimeClockAdjustment> adjustments;
        private String tenantId;
        private String employeeId;
        private Instant fromInclusive;
        private Instant toExclusive;
        private Collection<UUID> eventIds = new ArrayList<>();

        private CapturingRepository(List<TimeClockEvent> events, List<TimeClockAdjustment> adjustments) {
            this.events = events;
            this.adjustments = adjustments;
        }

        @Override
        public List<TimeClockEvent> findOriginalEvents(String tenantId, String employeeId, Instant fromInclusive, Instant toExclusive) {
            this.tenantId = tenantId;
            this.employeeId = employeeId;
            this.fromInclusive = fromInclusive;
            this.toExclusive = toExclusive;
            return events;
        }

        @Override
        public List<TimeClockAdjustment> findApprovedAdjustments(String tenantId, String employeeId, Collection<UUID> originalClientEventIds) {
            this.eventIds = List.copyOf(originalClientEventIds);
            return adjustments;
        }
    }
}
