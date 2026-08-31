package com.example.timetracking.adjustment.persistence;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import com.example.timetracking.adjustment.TimeClockAdjustmentStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        validateKey(tenantId, id);
        return repository.findByTenantIdAndId(tenantId, id).map(TimeClockAdjustmentEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<TimeClockAdjustment> decideIfPending(
            String tenantId,
            UUID id,
            TimeClockAdjustmentStatus decision,
            String actor,
            Instant decidedAt
    ) {
        validateKey(tenantId, id);
        Objects.requireNonNull(decision, "decision");
        if (decision == TimeClockAdjustmentStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("decision must be terminal");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        Objects.requireNonNull(decidedAt, "decidedAt");

        int updated = repository.decideIfPending(
                tenantId,
                id,
                TimeClockAdjustmentStatus.PENDING_APPROVAL,
                decision,
                actor,
                decidedAt
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return repository.findByTenantIdAndId(tenantId, id).map(TimeClockAdjustmentEntity::toDomain);
    }

    private void validateKey(String tenantId, UUID id) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        Objects.requireNonNull(id, "id");
    }
}
