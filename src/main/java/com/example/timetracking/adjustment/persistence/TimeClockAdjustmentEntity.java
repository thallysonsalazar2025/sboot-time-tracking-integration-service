package com.example.timetracking.adjustment.persistence;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_clock_adjustment")
public class TimeClockAdjustmentEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "employee_id", nullable = false, length = 100)
    private String employeeId;

    @Column(name = "original_client_event_id", nullable = false)
    private UUID originalClientEventId;

    @Column(nullable = false, length = 1000)
    private String justification;

    @Column(name = "requested_by", nullable = false, length = 150)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TimeClockAdjustmentStatus status;

    @Column(name = "decided_by", length = 150)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected TimeClockAdjustmentEntity() {
    }

    private TimeClockAdjustmentEntity(TimeClockAdjustment adjustment) {
        this.id = adjustment.id();
        this.tenantId = adjustment.tenantId();
        this.employeeId = adjustment.employeeId();
        this.originalClientEventId = adjustment.originalClientEventId();
        this.justification = adjustment.justification();
        this.requestedBy = adjustment.requestedBy();
        this.requestedAt = adjustment.requestedAt();
        this.status = adjustment.status();
        this.decidedBy = adjustment.decidedBy();
        this.decidedAt = adjustment.decidedAt();
    }

    public static TimeClockAdjustmentEntity from(TimeClockAdjustment adjustment) {
        return new TimeClockAdjustmentEntity(adjustment);
    }

    public TimeClockAdjustment toDomain() {
        return new TimeClockAdjustment(
                id,
                tenantId,
                employeeId,
                originalClientEventId,
                justification,
                requestedBy,
                requestedAt,
                status,
                decidedBy,
                decidedAt
        );
    }
}
