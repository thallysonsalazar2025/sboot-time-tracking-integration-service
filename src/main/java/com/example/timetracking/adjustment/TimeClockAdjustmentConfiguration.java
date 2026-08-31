package com.example.timetracking.adjustment;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeClockAdjustmentConfiguration {

    @Bean
    Clock timeClockAdjustmentClock() {
        return Clock.systemUTC();
    }

    @Bean
    TimeClockAdjustmentService timeClockAdjustmentService(
            TimeClockAdjustmentStore store,
            Clock timeClockAdjustmentClock,
            TimeClockAdjustmentEventPublisher eventPublisher
    ) {
        return new TimeClockAdjustmentService(store, timeClockAdjustmentClock, eventPublisher);
    }
}
