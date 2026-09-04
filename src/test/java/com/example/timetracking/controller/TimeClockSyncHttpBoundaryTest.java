package com.example.timetracking.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.timetracking.clock.InMemoryTimeClockEventStore;
import com.example.timetracking.clock.TimeClockBatchSyncService;
import com.example.timetracking.config.CorrelationIdFilter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class TimeClockSyncHttpBoundaryTest {

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        TimeClockBatchSyncService syncService = new TimeClockBatchSyncService(new InMemoryTimeClockEventStore());
        client = WebTestClient.bindToController(new TimeClockSyncController(syncService))
                .webFilter(new CorrelationIdFilter())
                .build();
    }

    @Test
    void trustedIdentityAcceptsOwnedEventAndPreservesCanonicalCorrelationId() {
        UUID eventId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        client.post()
                .uri("/api/time-clock/events/sync")
                .header("X-Authenticated-Tenant-Id", "tenant-a")
                .header("X-Authenticated-Employee-Id", "employee-a")
                .header(CorrelationIdFilter.CORRELATION_ID, correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("[{\"employeeId\":\"employee-a\",\"clientEventId\":\"" + eventId
                        + "\",\"occurredAt\":\"2026-09-04T12:00:00Z\"}]")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIdFilter.CORRELATION_ID, correlationId)
                .expectBody()
                .jsonPath("$[0].clientEventId").isEqualTo(eventId.toString())
                .jsonPath("$[0].status").isEqualTo("ACCEPTED");
    }

    @Test
    void employeeCannotSyncEventOwnedByAnotherEmployee() {
        UUID eventId = UUID.randomUUID();

        client.post()
                .uri("/api/time-clock/events/sync")
                .header("X-Authenticated-Tenant-Id", "tenant-a")
                .header("X-Authenticated-Employee-Id", "employee-a")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("[{\"employeeId\":\"employee-b\",\"clientEventId\":\"" + eventId
                        + "\",\"occurredAt\":\"2026-09-04T12:00:00Z\"}]")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void missingTrustedTenantHeaderFailsAtHttpBoundary() {
        UUID eventId = UUID.randomUUID();

        client.post()
                .uri("/api/time-clock/events/sync")
                .header("X-Authenticated-Employee-Id", "employee-a")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("[{\"employeeId\":\"employee-a\",\"clientEventId\":\"" + eventId
                        + "\",\"occurredAt\":\"2026-09-04T12:00:00Z\"}]")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void malformedCorrelationIdIsReplacedWithServerUuid() {
        UUID eventId = UUID.randomUUID();

        client.post()
                .uri("/api/time-clock/events/sync")
                .header("X-Authenticated-Tenant-Id", "tenant-a")
                .header("X-Authenticated-Employee-Id", "employee-a")
                .header(CorrelationIdFilter.CORRELATION_ID, "spoofed-correlation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("[{\"employeeId\":\"employee-a\",\"clientEventId\":\"" + eventId
                        + "\",\"occurredAt\":\"2026-09-04T12:00:00Z\"}]")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(CorrelationIdFilter.CORRELATION_ID, value -> {
                    assertThat(value).isNotEqualTo("spoofed-correlation");
                    assertThat(UUID.fromString(value).toString()).isEqualTo(value);
                });
    }
}
