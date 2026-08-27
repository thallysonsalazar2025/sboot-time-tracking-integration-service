package com.example.timetracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldPreserveProvidedCorrelationIdInResponseAndReactorContext() {
        String correlationId = "correlation-from-gateway";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/integrations/events")
                        .header(CorrelationIdFilter.CORRELATION_ID, correlationId)
                        .build());
        AtomicReference<String> contextValue = new AtomicReference<>();

        filter.filter(exchange, currentExchange -> Mono.deferContextual(context -> {
                    contextValue.set(context.get(CorrelationIdFilter.CORRELATION_ID));
                    return Mono.empty();
                }))
                .block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID))
                .isEqualTo(correlationId);
        assertThat(contextValue.get()).isEqualTo(correlationId);
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/integrations/events").build());

        filter.filter(exchange, currentExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID))
                .isNotBlank();
    }
}
