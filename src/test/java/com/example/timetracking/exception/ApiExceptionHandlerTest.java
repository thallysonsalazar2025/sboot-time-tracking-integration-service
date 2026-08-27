package com.example.timetracking.exception;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldMapProviderFailureToBadGateway() {
        ResponseEntity<Map<String, Object>> response = handler.handleExternalError(
                new ExternalIntegrationException("provider unavailable", new RuntimeException("boom")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("status", 502);
        assertThat(response.getBody()).containsEntry("message", "provider unavailable");
    }
}
