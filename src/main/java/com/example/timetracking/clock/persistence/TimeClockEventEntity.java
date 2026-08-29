package com.example.timetracking.clock.persistence;

import com.example.timetracking.clock.TimeClockEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "time_clock_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_time_clock_event_identity",
                columnNames = {"tenant_id", "employee_id", "client_event_id"}
        )
)
public class TimeClockEventEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "employee_id", nullable = false, length = 100)
    private String employeeId;

    @Column(name = "client_event_id", nullable = false)
    private UUID clientEventId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected TimeClockEventEntity() {
    }

    private TimeClockEventEntity(UUID id, String tenantId, String employeeId, UUID clientEventId, Instant occurredAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.clientEventId = clientEventId;
        this.occurredAt = occurredAt;
    }

    public static TimeClockEventEntity from(TimeClockEvent event) {
        return new TimeClockEventEntity(
                UUID.randomUUID(),
                event.tenantId(),
                event.employeeId(),
                event.clientEventId(),
                event.occurredAt()
        );
    }

    public TimeClockEvent toDomain() {
        return new TimeClockEvent(tenantId, employeeId, clientEventId, occurredAt);
    }
}
