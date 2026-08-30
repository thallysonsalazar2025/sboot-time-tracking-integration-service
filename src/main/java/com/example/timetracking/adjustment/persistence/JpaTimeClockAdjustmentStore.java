package com.example.timetracking.adjustment.persistence;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTimeClockAdjustmentStore implements TimeClockAdjustmentStore {
    private final SpringDataTimeClockAdjustmentRepository repository;

    public JpaTimeClockAdjustmentStore(SpringDataTimeClockAdjustmentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TimeClockAdjustment save(TimeClockAdjustment adjustment) {
        Objects.requireNonNull(adjustment, "adjustment");
        return repository.saveAndFlush(TimeClockAdjustmentEntity.from(adjustment)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TimeClockAdjustment> findByTenantIdAndId(String tenantId, UUID id) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        Objects.requireNonNull(id, "id");
        return repository.findByTenantIdAndId(tenantId, id).map(TimeClockAdjustmentEntity::toDomain);
    }
}
