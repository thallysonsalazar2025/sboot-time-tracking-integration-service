package com.example.timetracking.controller;

import com.example.timetracking.clock.TimeClockBatchSyncService;
import com.example.timetracking.clock.persistence.JpaTimeClockEventStore;
import com.example.timetracking.clock.persistence.TimeClockEventInserter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaTimeClockEventStore.class, TimeClockEventInserter.class})
@Testcontainers
class TimeClockSyncPostgresHttpTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JpaTimeClockEventStore store;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from time_clock_event");
        client = WebTestClient.bindToController(
                        new TimeClockSyncController(new TimeClockBatchSyncService(store)))
                .build();
    }

    @Test
    void sameEmployeeAndClientEventIdRemainIdempotentPerTrustedTenantInPostgres() {
        UUID clientEventId = UUID.randomUUID();

        sync("tenant-a", "employee-1", clientEventId, "CREATED");
        sync("tenant-a", "employee-1", clientEventId, "EXISTING");
        sync("tenant-b", "employee-1", clientEventId, "CREATED");

        Long rows = jdbcTemplate.queryForObject(
                "select count(*) from time_clock_event where client_event_id = ?",
                Long.class,
                clientEventId
        );
        List<String> tenants = jdbcTemplate.queryForList(
                "select tenant_id from time_clock_event where client_event_id = ? order by tenant_id",
                String.class,
                clientEventId
        );

        assertThat(rows).isEqualTo(2L);
        assertThat(tenants).containsExactly("tenant-a", "tenant-b");
    }

    private void sync(String tenantId, String employeeId, UUID clientEventId, String expectedStatus) {
        client.post()
                .uri("/api/time-clock/events/sync")
                .header("X-Authenticated-Tenant-Id", tenantId)
                .header("X-Authenticated-Employee-Id", employeeId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("[{\"employeeId\":\"" + employeeId + "\",\"clientEventId\":\"" + clientEventId
                        + "\",\"occurredAt\":\"2026-09-05T07:00:00Z\"}]")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].clientEventId").isEqualTo(clientEventId.toString())
                .jsonPath("$[0].status").isEqualTo(expectedStatus);
    }
}
