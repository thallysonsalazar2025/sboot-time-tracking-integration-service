package com.example.timetracking.clock.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTimeClockEventRepository extends JpaRepository<TimeClockEventEntity, UUID> {
    Optional<TimeClockEventEntity> findByTenantIdAndEmployeeIdAndClientEventId(
            String tenantId,
            String employeeId,
            UUID clientEventId
    );
}
