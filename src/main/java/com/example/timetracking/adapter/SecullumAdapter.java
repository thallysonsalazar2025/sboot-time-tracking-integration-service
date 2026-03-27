package com.example.timetracking.adapter;

import com.example.timetracking.client.SecullumClient;
import com.example.timetracking.domain.TimeEvent;
import com.example.timetracking.exception.ExternalIntegrationException;
import com.example.timetracking.mapper.SecullumMapper;
import com.example.timetracking.service.TimeTrackingProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SecullumAdapter implements TimeTrackingProvider {

    private static final Logger logger = LoggerFactory.getLogger(SecullumAdapter.class);

    private final SecullumClient secullumClient;
    private final SecullumMapper secullumMapper;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public SecullumAdapter(
            SecullumClient secullumClient,
            SecullumMapper secullumMapper,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.secullumClient = secullumClient;
        this.secullumMapper = secullumMapper;
        this.retry = retryRegistry.retry("secullum");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("secullum");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("secullum");
    }

    @Override
    public Flux<TimeEvent> fetchEvents(UUID companyId, YearMonth period) {
        logger.info("[SecullumAdapter] Starting external fetch companyId={} period={}", companyId, period);

        Mono<List<com.example.timetracking.dto.SecullumResponse>> securedCall = secullumClient
                .fetchEvents(companyId, period)
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));

        return securedCall
                .flatMapMany(Flux::fromIterable)
                .map(secullumMapper::toTimeEvent)
                .doOnNext(event -> logger.debug("[SecullumAdapter] Normalized event type={} date={}",
                        event.getType(), event.getDate()))
                .doOnComplete(() -> logger.info("[SecullumAdapter] Finished fetch companyId={} period={}",
                        companyId, period))
                .doOnError(ex -> logger.error("[SecullumAdapter] External integration failure", ex))
                .onErrorMap(ex -> new ExternalIntegrationException("Failed to fetch events from Secullum", ex));
    }
}
