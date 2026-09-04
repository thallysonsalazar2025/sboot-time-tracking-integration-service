package com.example.timetracking.observability;

import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Establishes one bounded correlation identifier per HTTP request.
 *
 * <p>An inbound identifier is reused only when it is a canonical UUID. Any
 * missing or malformed value is replaced with a server-generated UUID. The
 * identifier is returned to the caller and exposed through Reactor Context for
 * downstream observability without adding tenant, employee or other PII.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CONTEXT_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = normalize(exchange.getRequest().getHeaders());
        exchange.getResponse().getHeaders().set(HEADER, correlationId);
        return chain.filter(exchange)
                .contextWrite(context -> context.put(CONTEXT_KEY, correlationId));
    }

    static String normalize(HttpHeaders headers) {
        String candidate = headers.getFirst(HEADER);
        if (candidate == null || candidate.isBlank()) {
            return UUID.randomUUID().toString();
        }

        try {
            UUID parsed = UUID.fromString(candidate);
            String canonical = parsed.toString();
            return canonical.equals(candidate) ? canonical : UUID.randomUUID().toString();
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID().toString();
        }
    }
}
