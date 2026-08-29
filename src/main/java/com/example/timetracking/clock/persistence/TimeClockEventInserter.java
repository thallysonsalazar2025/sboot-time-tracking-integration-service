package com.example.timetracking.clock.persistence;

import com.example.timetracking.clock.TimeClockEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class TimeClockEventInserter {
    private final SpringDataTimeClockEventRepository repository;

    TimeClockEventInserter(SpringDataTimeClockEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    TimeClockEventEntity insert(TimeClockEvent event) {
        return repository.saveAndFlush(TimeClockEventEntity.from(event));
    }
}
