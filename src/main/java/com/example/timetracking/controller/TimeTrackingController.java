package com.example.timetracking.controller;

import com.example.timetracking.domain.TimeEvent;
import com.example.timetracking.service.TimeTrackingIntegrationService;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/integrations")
public class TimeTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingController.class);
    private final TimeTrackingIntegrationService service;

    public TimeTrackingController(TimeTrackingIntegrationService service) {
        this.service = service;
    }

    @GetMapping("/events")
    public Flux<TimeEvent> getEvents(
            @RequestParam @NotNull UUID companyId,
            @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth period) {

        logger.info("Received events request companyId={} period={}", companyId, period);
        return service.fetchEvents(companyId, period)
                .doOnComplete(() -> logger.info("Finished events request companyId={} period={}", companyId, period));
    }
}
