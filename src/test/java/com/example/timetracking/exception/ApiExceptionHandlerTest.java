package com.example.timetracking.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.timetracking.adjustment.TimeClockAdjustmentDecisionConflictException;
import com.example.timetracking.adjustment.TimeClockAdjustmentNotFoundException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void shouldMapMissingConfigurationToNotFound() {
        UUID companyId = UUID.randomUUID();

        ResponseEntity<Map<String, Object>> response = handler.handleConfigNotFound(
                new IntegrationConfigNotFoundException(companyId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody().get("message").toString()).contains(companyId.toString());
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void shouldMapAdjustmentNotFoundToNotFound() {
        ResponseEntity<Map<String, Object>> response = handler.handleAdjustmentNotFound(
                new TimeClockAdjustmentNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("message", "adjustment not found");
    }

    @Test
    void shouldMapContradictoryAdjustmentDecisionToConflict() {
        ResponseEntity<Map<String, Object>> response = handler.handleAdjustmentConflict(
                new TimeClockAdjustmentDecisionConflictException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("message", "adjustment already decided");
    }

    @Test
    void shouldMapProviderFailureToBadGateway() {
        ResponseEntity<Map<String, Object>> response = handler.handleExternalError(
                new ExternalIntegrationException("provider unavailable", new RuntimeException("boom")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("status", 502);
        assertThat(response.getBody()).containsEntry("message", "provider unavailable");
    }
}
