package com.example.timetracking.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentService;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class TimeClockAdjustmentControllerTest {

    private TimeClockAdjustmentService service;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(TimeClockAdjustmentService.class);
        client = WebTestClient.bindToController(new TimeClockAdjustmentController(service)).build();
    }

    @Test
    void decidesUsingOnlyTrustedTenantAndActorHeaders() {
        UUID adjustmentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-08-31T01:00:00Z");
        Instant decidedAt = Instant.parse("2026-08-31T01:01:00Z");
        TimeClockAdjustment approved = new TimeClockAdjustment(
                adjustmentId,
                "tenant-a",
                "employee-1",
                eventId,
                "Forgotten punch",
                "employee-1",
                requestedAt,
                TimeClockAdjustmentStatus.APPROVED,
                "manager-7",
                decidedAt);

        when(service.decide("tenant-a", adjustmentId, TimeClockAdjustmentStatus.APPROVED, "manager-7"))
                .thenReturn(approved);

        client.patch()
                .uri("/api/time-clock/adjustments/{id}/decision", adjustmentId)
                .header(TimeClockAdjustmentController.TRUSTED_TENANT_HEADER, "tenant-a")
                .header(TimeClockAdjustmentController.TRUSTED_ACTOR_HEADER, "manager-7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"decision\":\"APPROVED\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.adjustmentId").isEqualTo(adjustmentId.toString())
                .jsonPath("$.tenantId").isEqualTo("tenant-a")
                .jsonPath("$.employeeId").isEqualTo("employee-1")
                .jsonPath("$.status").isEqualTo("APPROVED")
                .jsonPath("$.decidedBy").isEqualTo("manager-7")
                .jsonPath("$.decidedAt").isEqualTo(decidedAt.toString());

        verify(service).decide("tenant-a", adjustmentId, TimeClockAdjustmentStatus.APPROVED, "manager-7");
    }

    @Test
    void rejectsRequestWithoutTrustedTenantHeader() {
        UUID adjustmentId = UUID.randomUUID();

        client.patch()
                .uri("/api/time-clock/adjustments/{id}/decision", adjustmentId)
                .header(TimeClockAdjustmentController.TRUSTED_ACTOR_HEADER, "manager-7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"decision\":\"REJECTED\"}")
                .exchange()
                .expectStatus().isBadRequest();

        Mockito.verifyNoInteractions(service);
    }
}
