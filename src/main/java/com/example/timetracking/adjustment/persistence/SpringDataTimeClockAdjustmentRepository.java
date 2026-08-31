package com.example.timetracking.adjustment.persistence;

import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface SpringDataTimeClockAdjustmentRepository extends JpaRepository<TimeClockAdjustmentEntity, UUID> {
    Optional<TimeClockAdjustmentEntity> findByTenantIdAndId(String tenantId, UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TimeClockAdjustmentEntity adjustment
               set adjustment.status = :decision,
                   adjustment.decidedBy = :actor,
                   adjustment.decidedAt = :decidedAt
             where adjustment.tenantId = :tenantId
               and adjustment.id = :id
               and adjustment.status = :pendingStatus
            """)
    int decideIfPending(
            @Param("tenantId") String tenantId,
            @Param("id") UUID id,
            @Param("pendingStatus") TimeClockAdjustmentStatus pendingStatus,
            @Param("decision") TimeClockAdjustmentStatus decision,
            @Param("actor") String actor,
            @Param("decidedAt") Instant decidedAt
    );
}
