package com.example.timetracking.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.timetracking.config.CorrelationIdFilter;
import java.time.YearMonth;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class SecullumClientTest {

    private MockWebServer server;
    private SecullumClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new SecullumClient(WebClient.builder(), server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void shouldPropagateCorrelationIdToSecullum() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        UUID companyId = UUID.randomUUID();
        String correlationId = "e2e-point-correlation-123";

        client.fetchEvents(companyId, YearMonth.of(2026, 8))
                .contextWrite(context -> context.put(CorrelationIdFilter.CORRELATION_ID, correlationId))
                .block();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader(CorrelationIdFilter.CORRELATION_ID)).isEqualTo(correlationId);
        assertThat(request.getRequestUrl().queryParameter("companyId")).isEqualTo(companyId.toString());
        assertThat(request.getRequestUrl().queryParameter("period")).isEqualTo("2026-08");
    }
}
