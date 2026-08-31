package com.example.timetracking.timesheet;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import com.example.timetracking.adjustment.persistence.TimeClockAdjustmentEntity;
import com.example.timetracking.clock.TimeClockEvent;
import com.example.timetracking.clock.persistence.TimeClockEventEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaTimesheetReadRepository implements TimesheetReadRepository {
    private final EntityManager entityManager;

    public JpaTimesheetReadRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<TimeClockEvent> findOriginalEvents(String tenantId, String employeeId, Instant fromInclusive, Instant toExclusive) {
        return entityManager.createQuery("""
                select event from TimeClockEventEntity event
                 where event.tenantId = :tenantId
                   and event.employeeId = :employeeId
                   and event.occurredAt >= :fromInclusive
                   and event.occurredAt < :toExclusive
                 order by event.occurredAt, event.clientEventId
                """, TimeClockEventEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("employeeId", employeeId)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getResultList()
                .stream()
                .map(TimeClockEventEntity::toDomain)
                .toList();
    }

    @Override
    public List<TimeClockAdjustment> findApprovedAdjustments(String tenantId, String employeeId, Collection<UUID> originalClientEventIds) {
        if (originalClientEventIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                select adjustment from TimeClockAdjustmentEntity adjustment
                 where adjustment.tenantId = :tenantId
                   and adjustment.employeeId = :employeeId
                   and adjustment.status = :approved
                   and adjustment.originalClientEventId in :eventIds
                 order by adjustment.requestedAt, adjustment.id
                """, TimeClockAdjustmentEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("employeeId", employeeId)
                .setParameter("approved", TimeClockAdjustmentStatus.APPROVED)
                .setParameter("eventIds", originalClientEventIds)
                .getResultList()
                .stream()
                .map(TimeClockAdjustmentEntity::toDomain)
                .toList();
    }
}
