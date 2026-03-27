package com.example.timetracking.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.timetracking.domain.IntegrationConfig;
import com.example.timetracking.domain.ProviderType;
import com.example.timetracking.domain.TimeEvent;
import com.example.timetracking.exception.IntegrationConfigNotFoundException;
import com.example.timetracking.repository.IntegrationConfigRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class TimeTrackingIntegrationServiceTest {

    private final IntegrationConfigRepository repository = Mockito.mock(IntegrationConfigRepository.class);
    private final ProviderResolver providerResolver = Mockito.mock(ProviderResolver.class);
    private final TimeTrackingProvider provider = Mockito.mock(TimeTrackingProvider.class);

    @Test
    void shouldFetchEventsUsingResolvedProvider() {
        TimeTrackingIntegrationService service = new TimeTrackingIntegrationService(repository, providerResolver);
        UUID companyId = UUID.randomUUID();

        IntegrationConfig config = new IntegrationConfig(UUID.randomUUID(), companyId, ProviderType.SECULLUM, "{}", true);
        when(repository.findByCompanyIdAndActiveTrue(companyId)).thenReturn(Optional.of(config));
        when(providerResolver.resolve(ProviderType.SECULLUM)).thenReturn(provider);
        when(provider.fetchEvents(companyId, YearMonth.of(2026, 3))).thenReturn(Flux.just(
                new TimeEvent("ABSENCE", LocalDate.of(2026, 3, 3), BigDecimal.ONE, BigDecimal.ZERO)));

        StepVerifier.create(service.fetchEvents(companyId, YearMonth.of(2026, 3)))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldFailWhenConfigMissing() {
        TimeTrackingIntegrationService service = new TimeTrackingIntegrationService(repository, providerResolver);
        UUID companyId = UUID.randomUUID();
        when(repository.findByCompanyIdAndActiveTrue(companyId)).thenReturn(Optional.empty());

        StepVerifier.create(service.fetchEvents(companyId, YearMonth.of(2026, 3)))
                .expectErrorSatisfies(ex -> assertTrue(ex instanceof IntegrationConfigNotFoundException))
                .verify();
    }
}
