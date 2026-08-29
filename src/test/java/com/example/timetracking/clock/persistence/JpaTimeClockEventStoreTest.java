package com.example.timetracking.clock.persistence;

import com.example.timetracking.clock.TimeClockEvent;
import com.example.timetracking.clock.TimeClockRegistrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@Import(JpaTimeClockEventStore.class)
class JpaTimeClockEventStoreTest {
    @Autowired
    private JpaTimeClockEventStore store;

    @Test
    void persistsIdempotentlyWithinTenantEmployeeBoundary() {
        UUID clientEventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-29T18:00:00Z");
        TimeClockEvent event = new TimeClockEvent("tenant-a", "employee-1", clientEventId, occurredAt);

        assertThat(store.register(event).status()).isEqualTo(TimeClockRegistrationStatus.CREATED);
        assertThat(store.register(event).status()).isEqualTo(TimeClockRegistrationStatus.EXISTING);
    }

    @Test
    void sameClientEventIdRemainsIndependentAcrossTenantsAndEmployees() {
        UUID clientEventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-29T18:00:00Z");

        assertThat(store.register(new TimeClockEvent("tenant-a", "employee-1", clientEventId, occurredAt)).status())
                .isEqualTo(TimeClockRegistrationStatus.CREATED);
        assertThat(store.register(new TimeClockEvent("tenant-b", "employee-1", clientEventId, occurredAt)).status())
                .isEqualTo(TimeClockRegistrationStatus.CREATED);
        assertThat(store.register(new TimeClockEvent("tenant-a", "employee-2", clientEventId, occurredAt)).status())
                .isEqualTo(TimeClockRegistrationStatus.CREATED);
    }
}
