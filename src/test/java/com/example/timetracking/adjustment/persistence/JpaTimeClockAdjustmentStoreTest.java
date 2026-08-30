package com.example.timetracking.adjustment.persistence;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=true"
})
@Import(JpaTimeClockAdjustmentStore.class)
class JpaTimeClockAdjustmentStoreTest {
    @Autowired
    private JpaTimeClockAdjustmentStore store;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAndReadsAdjustmentOnlyInsideTrustedTenantScope() {
        UUID originalClientEventId = UUID.randomUUID();
        insertOriginalEvent("tenant-a", "employee-1", originalClientEventId);
        TimeClockAdjustment adjustment = TimeClockAdjustment.request(
                "tenant-a",
                "employee-1",
                originalClientEventId,
                "Corrigir omissão no espelho",
                "employee-1",
                Instant.parse("2026-08-30T11:30:00Z")
        );

        TimeClockAdjustment saved = store.save(adjustment);

        assertEquals(adjustment.id(), saved.id());
        assertEquals(TimeClockAdjustmentStatus.PENDING_APPROVAL, saved.status());
        assertTrue(store.findByTenantIdAndId("tenant-a", saved.id()).isPresent());
        assertTrue(store.findByTenantIdAndId("tenant-b", saved.id()).isEmpty());
    }

    @Test
    void rejectsCrossTenantReferenceToOriginalEvent() {
        UUID originalClientEventId = UUID.randomUUID();
        insertOriginalEvent("tenant-a", "employee-1", originalClientEventId);
        TimeClockAdjustment spoofed = TimeClockAdjustment.request(
                "tenant-b",
                "employee-1",
                originalClientEventId,
                "Tentativa de referência cruzada",
                "employee-1",
                Instant.parse("2026-08-30T11:31:00Z")
        );

        assertThrows(DataIntegrityViolationException.class, () -> store.save(spoofed));
    }

    @Test
    void rejectsBlankTenantLookup() {
        assertThrows(IllegalArgumentException.class,
                () -> store.findByTenantIdAndId(" ", UUID.randomUUID()));
    }

    private void insertOriginalEvent(String tenantId, String employeeId, UUID clientEventId) {
        jdbcTemplate.update(
                "INSERT INTO time_clock_event (id, tenant_id, employee_id, client_event_id, occurred_at) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                tenantId,
                employeeId,
                clientEventId,
                Timestamp.from(Instant.parse("2026-08-30T11:00:00Z"))
        );
    }
}
