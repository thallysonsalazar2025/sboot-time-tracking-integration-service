package com.example.timetracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldPreserveCanonicalUuidInResponseAndReactorContext() {
        String correlationId = UUID.randomUUID().toString();
        MockServerWebExchange exchange = exchange(correlationId);
        AtomicReference<String> contextValue = new AtomicReference<>();

        filter.filter(exchange, currentExchange -> Mono.deferContextual(context -> {
                    contextValue.set(context.get(CorrelationIdFilter.CORRELATION_ID));
                    return Mono.empty();
                }))
                .block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID))
                .isEqualTo(correlationId);
        assertThat(contextValue.get()).isEqualTo(correlationId);
        assertThat(exchange.getResponse().getHeaders().get(CorrelationIdFilter.CORRELATION_ID)).hasSize(1);
    }

    @Test
    void shouldGenerateCanonicalUuidWhenHeaderIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/integrations/events").build());
        AtomicReference<String> contextValue = new AtomicReference<>();

        filter.filter(exchange, currentExchange -> Mono.deferContextual(context -> {
            contextValue.set(context.get(CorrelationIdFilter.CORRELATION_ID));
            return Mono.empty();
        })).block();

        String generated = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID);
        assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
        assertThat(contextValue).hasValue(generated);
    }

    @Test
    void shouldReplaceMalformedInboundValueInsteadOfPropagatingIt() {
        MockServerWebExchange exchange = exchange("spoofed-correlation-id");
        AtomicReference<String> contextValue = new AtomicReference<>();

        filter.filter(exchange, currentExchange -> Mono.deferContextual(context -> {
            contextValue.set(context.get(CorrelationIdFilter.CORRELATION_ID));
            return Mono.empty();
        })).block();

        String generated = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID);
        assertThat(generated).isNotEqualTo("spoofed-correlation-id");
        assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
        assertThat(contextValue).hasValue(generated);
    }

    @Test
    void shouldReplaceNonCanonicalUuidText() {
        String uppercase = UUID.randomUUID().toString().toUpperCase();
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationIdFilter.CORRELATION_ID, uppercase);

        String normalized = CorrelationIdFilter.normalize(headers);

        assertThat(normalized).isNotEqualTo(uppercase);
        assertThat(UUID.fromString(normalized).toString()).isEqualTo(normalized);
    }

    private static MockServerWebExchange exchange(String correlationId) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/integrations/events")
                        .header(CorrelationIdFilter.CORRELATION_ID, correlationId)
                        .build());
    }
}
