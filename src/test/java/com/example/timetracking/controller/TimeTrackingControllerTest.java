package com.example.timetracking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.timetracking.domain.TimeEvent;
import com.example.timetracking.service.TimeTrackingIntegrationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class TimeTrackingControllerTest {

    @Test
    void shouldDelegateEventsQueryToService() {
        TimeTrackingIntegrationService service = Mockito.mock(TimeTrackingIntegrationService.class);
        TimeTrackingController controller = new TimeTrackingController(service);
        UUID companyId = UUID.randomUUID();
        YearMonth period = YearMonth.of(2026, 8);
        TimeEvent event = new TimeEvent("OVERTIME", LocalDate.of(2026, 8, 27), BigDecimal.ONE, BigDecimal.TEN);
        when(service.fetchEvents(companyId, period)).thenReturn(Flux.just(event));

        StepVerifier.create(controller.getEvents(companyId, period))
                .assertNext(actual -> assertThat(actual).isSameAs(event))
                .verifyComplete();
    }
}
