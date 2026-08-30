package com.example.timetracking.adjustment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTimeClockAdjustmentRepository extends JpaRepository<TimeClockAdjustmentEntity, UUID> {
    Optional<TimeClockAdjustmentEntity> findByTenantIdAndId(String tenantId, UUID id);
}
