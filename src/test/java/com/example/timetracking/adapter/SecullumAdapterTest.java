package com.example.timetracking.adapter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.timetracking.client.SecullumClient;
import com.example.timetracking.dto.SecullumResponse;
import com.example.timetracking.exception.ExternalIntegrationException;
import com.example.timetracking.mapper.SecullumMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SecullumAdapterTest {

    private final SecullumClient client = Mockito.mock(SecullumClient.class);
    private final SecullumMapper mapper = new SecullumMapper();
    private final RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private final CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
    private final TimeLimiterRegistry tlRegistry = TimeLimiterRegistry.ofDefaults();

    @Test
    void shouldReturnMappedEvents() {
        SecullumAdapter adapter = new SecullumAdapter(client, mapper, retryRegistry, cbRegistry, tlRegistry);

        when(client.fetchEvents(any(), any())).thenReturn(Mono.just(List.of(
                new SecullumResponse("OVERTIME", LocalDate.of(2026, 3, 1), BigDecimal.TEN, BigDecimal.ONE))));

        StepVerifier.create(adapter.fetchEvents(UUID.randomUUID(), YearMonth.of(2026, 3)))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldWrapExternalFailure() {
        SecullumAdapter adapter = new SecullumAdapter(client, mapper, retryRegistry, cbRegistry, tlRegistry);

        when(client.fetchEvents(any(), any())).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(adapter.fetchEvents(UUID.randomUUID(), YearMonth.of(2026, 3)))
                .expectErrorSatisfies(ex -> assertTrue(ex instanceof ExternalIntegrationException))
                .verify();

        verify(client, times(3)).fetchEvents(any(), any());
    }

    @Test
    void shouldTimeoutByTimeLimiter() {
        TimeLimiterRegistry shortTlRegistry = TimeLimiterRegistry.of(
                io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofMillis(30))
                        .build());
        SecullumAdapter adapter = new SecullumAdapter(client, mapper, retryRegistry, cbRegistry, shortTlRegistry);

        when(client.fetchEvents(any(), any())).thenReturn(Mono.delay(Duration.ofMillis(100)).thenReturn(List.of()));

        StepVerifier.create(adapter.fetchEvents(UUID.randomUUID(), YearMonth.of(2026, 3)))
                .expectError(ExternalIntegrationException.class)
                .verify();
    }
}
