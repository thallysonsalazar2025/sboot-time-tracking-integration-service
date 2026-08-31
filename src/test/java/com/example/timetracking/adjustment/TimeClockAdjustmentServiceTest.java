package com.example.timetracking.adjustment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimeClockAdjustmentServiceTest {
    private static final Instant REQUESTED_AT = Instant.parse("2026-08-30T20:00:00Z");
    private static final Instant DECIDED_AT = Instant.parse("2026-08-30T21:00:00Z");

    @Test
    void decidesPendingAdjustmentInsideTenantAndPersistsDecision() {
        InMemoryStore store = new InMemoryStore();
        TimeClockAdjustment pending = TimeClockAdjustment.request(
                "tenant-a", "employee-1", UUID.randomUUID(), "forgot exit", "employee-1", REQUESTED_AT);
        store.save(pending);
        var service = new TimeClockAdjustmentService(store, Clock.fixed(DECIDED_AT, ZoneOffset.UTC));

        TimeClockAdjustment approved = service.decide(
                "tenant-a", pending.id(), TimeClockAdjustmentStatus.APPROVED, "rh-user");

        assertEquals(TimeClockAdjustmentStatus.APPROVED, approved.status());
        assertEquals("rh-user", approved.decidedBy());
        assertEquals(DECIDED_AT, approved.decidedAt());
        assertSame(approved, store.findByTenantIdAndId("tenant-a", pending.id()).orElseThrow());
    }

    @Test
    void sameTerminalDecisionIsIdempotentAndDoesNotRewriteAuditMetadata() {
        InMemoryStore store = new InMemoryStore();
        TimeClockAdjustment pending = TimeClockAdjustment.request(
                "tenant-a", "employee-1", UUID.randomUUID(), "forgot exit", "employee-1", REQUESTED_AT);
        TimeClockAdjustment approved = pending.decide(
                TimeClockAdjustmentStatus.APPROVED, "rh-user", DECIDED_AT);
        store.save(approved);
        var service = new TimeClockAdjustmentService(
                store, Clock.fixed(DECIDED_AT.plusSeconds(3600), ZoneOffset.UTC));

        TimeClockAdjustment replay = service.decide(
                "tenant-a", pending.id(), TimeClockAdjustmentStatus.APPROVED, "another-rh-user");

        assertSame(approved, replay);
        assertEquals("rh-user", replay.decidedBy());
        assertEquals(DECIDED_AT, replay.decidedAt());
        assertEquals(1, store.saveCount);
    }

    @Test
    void rejectsCrossTenantLookupAndContradictorySecondDecision() {
        InMemoryStore store = new InMemoryStore();
        TimeClockAdjustment pending = TimeClockAdjustment.request(
                "tenant-a", "employee-1", UUID.randomUUID(), "forgot exit", "employee-1", REQUESTED_AT);
        store.save(pending);
        var service = new TimeClockAdjustmentService(store, Clock.fixed(DECIDED_AT, ZoneOffset.UTC));

        assertThrows(IllegalArgumentException.class, () -> service.decide(
                "tenant-b", pending.id(), TimeClockAdjustmentStatus.APPROVED, "rh-b"));

        service.decide("tenant-a", pending.id(), TimeClockAdjustmentStatus.APPROVED, "rh-a");
        assertThrows(IllegalStateException.class, () -> service.decide(
                "tenant-a", pending.id(), TimeClockAdjustmentStatus.REJECTED, "rh-a"));
    }

    @Test
    void losingConcurrentDecisionReloadsCommittedState() {
        InMemoryStore store = new InMemoryStore();
        TimeClockAdjustment pending = TimeClockAdjustment.request(
                "tenant-a", "employee-1", UUID.randomUUID(), "forgot exit", "employee-1", REQUESTED_AT);
        store.save(pending);
        store.concurrentWinner = pending.decide(
                TimeClockAdjustmentStatus.APPROVED, "rh-winner", DECIDED_AT.minusSeconds(1));
        var service = new TimeClockAdjustmentService(store, Clock.fixed(DECIDED_AT, ZoneOffset.UTC));

        TimeClockAdjustment replay = service.decide(
                "tenant-a", pending.id(), TimeClockAdjustmentStatus.APPROVED, "rh-loser");

        assertEquals("rh-winner", replay.decidedBy());
        assertEquals(DECIDED_AT.minusSeconds(1), replay.decidedAt());
    }

    private static final class InMemoryStore implements TimeClockAdjustmentStore {
        private final Map<String, TimeClockAdjustment> data = new HashMap<>();
        private int saveCount;
        private TimeClockAdjustment concurrentWinner;

        @Override
        public synchronized TimeClockAdjustment save(TimeClockAdjustment adjustment) {
            saveCount++;
            data.put(key(adjustment.tenantId(), adjustment.id()), adjustment);
            return adjustment;
        }

        @Override
        public synchronized Optional<TimeClockAdjustment> findByTenantIdAndId(String tenantId, UUID id) {
            return Optional.ofNullable(data.get(key(tenantId, id)));
        }

        @Override
        public synchronized Optional<TimeClockAdjustment> decideIfPending(
                String tenantId,
                UUID id,
                TimeClockAdjustmentStatus decision,
                String actor,
                Instant decidedAt
        ) {
            String key = key(tenantId, id);
            TimeClockAdjustment current = data.get(key);
            if (current == null || current.status() != TimeClockAdjustmentStatus.PENDING_APPROVAL) {
                return Optional.empty();
            }
            if (concurrentWinner != null) {
                data.put(key, concurrentWinner);
                concurrentWinner = null;
                return Optional.empty();
            }
            TimeClockAdjustment decided = current.decide(decision, actor, decidedAt);
            data.put(key, decided);
            return Optional.of(decided);
        }

        private String key(String tenantId, UUID id) {
            return tenantId + ":" + id;
        }
    }
}
