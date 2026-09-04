package com.example.timetracking.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

class CorrelationIdWebFilterTest {

    private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

    @Test
    void preservesCanonicalUuidAndPropagatesItToResponseAndReactorContext() {
        String correlationId = UUID.randomUUID().toString();
        MockServerWebExchange exchange = exchange(correlationId);
        AtomicReference<String> seen = new AtomicReference<>();
        WebFilterChain chain = currentCorrelationId(seen);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.HEADER))
                .isEqualTo(correlationId);
        assertThat(seen).hasValue(correlationId);
    }

    @Test
    void replacesMalformedInboundValueWithServerGeneratedUuid() {
        MockServerWebExchange exchange = exchange("not-a-uuid");
        AtomicReference<String> seen = new AtomicReference<>();

        filter.filter(exchange, currentCorrelationId(seen)).block();

        String generated = exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.HEADER);
        assertThat(generated).isNotEqualTo("not-a-uuid");
        assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
        assertThat(seen).hasValue(generated);
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/time-clock/events/sync"));
        AtomicReference<String> seen = new AtomicReference<>();

        filter.filter(exchange, currentCorrelationId(seen)).block();

        String generated = exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.HEADER);
        assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
        assertThat(seen).hasValue(generated);
    }

    @Test
    void rejectsNonCanonicalUuidTextInsteadOfNormalizingUntrustedInput() {
        String uppercase = UUID.randomUUID().toString().toUpperCase();
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationIdWebFilter.HEADER, uppercase);

        String normalized = CorrelationIdWebFilter.normalize(headers);

        assertThat(normalized).isNotEqualTo(uppercase);
        assertThat(UUID.fromString(normalized).toString()).isEqualTo(normalized);
    }

    private static MockServerWebExchange exchange(String correlationId) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/time-clock/events/sync")
                        .header(CorrelationIdWebFilter.HEADER, correlationId));
    }

    private static WebFilterChain currentCorrelationId(AtomicReference<String> seen) {
        return ignored -> Mono.deferContextual(context -> capture(context, seen));
    }

    private static Mono<Void> capture(ContextView context, AtomicReference<String> seen) {
        seen.set(context.get(CorrelationIdWebFilter.CONTEXT_KEY));
        return Mono.empty();
    }
}
