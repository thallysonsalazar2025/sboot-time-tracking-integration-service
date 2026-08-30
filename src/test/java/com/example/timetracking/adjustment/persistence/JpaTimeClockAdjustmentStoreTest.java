package com.example.timetracking.adjustment.persistence;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@Import(JpaTimeClockAdjustmentStore.class)
class JpaTimeClockAdjustmentStoreTest {
    @Autowired
    private JpaTimeClockAdjustmentStore store;

    @Test
    void persistsAndReadsAdjustmentOnlyInsideTrustedTenantScope() {
        TimeClockAdjustment adjustment = TimeClockAdjustment.request(
                "tenant-a",
                "employee-1",
                UUID.randomUUID(),
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
}
