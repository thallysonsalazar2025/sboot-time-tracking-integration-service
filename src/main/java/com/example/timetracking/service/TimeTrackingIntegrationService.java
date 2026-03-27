package com.example.timetracking.service;

import com.example.timetracking.domain.IntegrationConfig;
import com.example.timetracking.domain.TimeEvent;
import com.example.timetracking.exception.IntegrationConfigNotFoundException;
import com.example.timetracking.repository.IntegrationConfigRepository;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class TimeTrackingIntegrationService {

    private final IntegrationConfigRepository configRepository;
    private final ProviderResolver providerResolver;

    public TimeTrackingIntegrationService(IntegrationConfigRepository configRepository, ProviderResolver providerResolver) {
        this.configRepository = configRepository;
        this.providerResolver = providerResolver;
    }

    public Flux<TimeEvent> fetchEvents(UUID companyId, YearMonth period) {
        return Mono.fromCallable(() -> configRepository.findByCompanyIdAndActiveTrue(companyId)
                        .orElseThrow(() -> new IntegrationConfigNotFoundException(companyId)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(config -> invokeProvider(companyId, period, config));
    }

    private Flux<TimeEvent> invokeProvider(UUID companyId, YearMonth period, IntegrationConfig config) {
        TimeTrackingProvider provider = providerResolver.resolve(config.getProviderType());
        return provider.fetchEvents(companyId, period);
    }
}
