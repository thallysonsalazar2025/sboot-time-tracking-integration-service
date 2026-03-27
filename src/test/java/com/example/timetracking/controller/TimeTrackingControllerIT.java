package com.example.timetracking.controller;

import com.example.timetracking.domain.IntegrationConfig;
import com.example.timetracking.domain.ProviderType;
import com.example.timetracking.repository.IntegrationConfigRepository;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimeTrackingControllerIT {

    private static MockWebServer mockWebServer;

    @LocalServerPort
    int port;

    @Autowired
    IntegrationConfigRepository repository;

    @BeforeAll
    static void beforeAll() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void afterAll() throws Exception {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("secullum.base-url", () -> mockWebServer.url("/").toString());
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldReturnNormalizedEvents() {
        UUID companyId = UUID.randomUUID();
        repository.save(new IntegrationConfig(UUID.randomUUID(), companyId, ProviderType.SECULLUM, "{}", true));

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {"eventType":"OVERTIME","eventDate":"2026-03-15","hours":2.5,"value":120.00}
                        ]
                        """));

        WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build()
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/integrations/events")
                        .queryParam("companyId", companyId)
                        .queryParam("period", "2026-03")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].type").isEqualTo("OVERTIME")
                .jsonPath("$[0].date").isEqualTo("2026-03-15")
                .jsonPath("$[0].quantity").isEqualTo(2.5)
                .jsonPath("$[0].amount").isEqualTo(120.00);
    }

    @Test
    void shouldReturnNotFoundWhenNoConfig() {
        WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build()
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/integrations/events")
                        .queryParam("companyId", UUID.randomUUID())
                        .queryParam("period", "2026-03")
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }
}
