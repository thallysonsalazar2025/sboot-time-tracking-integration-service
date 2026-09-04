package com.example.timetracking.config;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = normalize(exchange.getRequest().getHeaders());
        exchange.getResponse().getHeaders().set(CORRELATION_ID, correlationId);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CORRELATION_ID, correlationId))
                .doFirst(() -> MDC.put(CORRELATION_ID, correlationId))
                .doFinally(signal -> MDC.remove(CORRELATION_ID));
    }

    static String normalize(HttpHeaders headers) {
        String candidate = headers.getFirst(CORRELATION_ID);
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
