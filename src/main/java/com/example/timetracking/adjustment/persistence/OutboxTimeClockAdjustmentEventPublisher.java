package com.example.timetracking.adjustment.persistence;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentEventPublisher;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OutboxTimeClockAdjustmentEventPublisher implements TimeClockAdjustmentEventPublisher {
    private static final String EVENT_TYPE = "TIME_CLOCK_ADJUSTMENT_APPROVED";

    private final JdbcTemplate jdbcTemplate;

    public OutboxTimeClockAdjustmentEventPublisher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void publishApproved(TimeClockAdjustment adjustment) {
        jdbcTemplate.update(
                """
                insert into time_clock_adjustment_outbox
                    (id, tenant_id, adjustment_id, event_type, employee_id, original_client_event_id,
                     decided_by, decided_at, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (tenant_id, adjustment_id, event_type) do nothing
                """,
                UUID.randomUUID(),
                adjustment.tenantId(),
                adjustment.id(),
                EVENT_TYPE,
                adjustment.employeeId(),
                adjustment.originalClientEventId(),
                adjustment.decidedBy(),
                Timestamp.from(adjustment.decidedAt()),
                Timestamp.from(adjustment.decidedAt())
        );
    }
}
