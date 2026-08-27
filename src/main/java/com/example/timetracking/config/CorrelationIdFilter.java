package com.example.timetracking.config;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestedCorrelationId = request.getHeaders().getFirst(CORRELATION_ID);
        String correlationId = requestedCorrelationId == null || requestedCorrelationId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedCorrelationId;

        exchange.getResponse().getHeaders().add(CORRELATION_ID, correlationId);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CORRELATION_ID, correlationId))
                .doFirst(() -> MDC.put(CORRELATION_ID, correlationId))
                .doFinally(signal -> MDC.remove(CORRELATION_ID));
    }
}
