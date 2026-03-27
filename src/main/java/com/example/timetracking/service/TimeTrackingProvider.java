package com.example.timetracking.service;

import com.example.timetracking.domain.TimeEvent;
import java.time.YearMonth;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface TimeTrackingProvider {
    Flux<TimeEvent> fetchEvents(UUID companyId, YearMonth period);
}
