package com.example.timetracking.clock.persistence;

import com.example.timetracking.clock.TimeClockEvent;
import com.example.timetracking.clock.TimeClockEventStore;
import com.example.timetracking.clock.TimeClockRegistrationResult;
import com.example.timetracking.clock.TimeClockRegistrationStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
public class JpaTimeClockEventStore implements TimeClockEventStore {
    private final SpringDataTimeClockEventRepository repository;

    public JpaTimeClockEventStore(SpringDataTimeClockEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TimeClockRegistrationResult register(TimeClockEvent event) {
        Objects.requireNonNull(event, "event");
        return repository.findByTenantIdAndEmployeeIdAndClientEventId(
                        event.tenantId(),
                        event.employeeId(),
                        event.clientEventId()
                )
                .map(existing -> new TimeClockRegistrationResult(
                        TimeClockRegistrationStatus.EXISTING,
                        existing.toDomain()
                ))
                .orElseGet(() -> {
                    TimeClockEventEntity saved = repository.saveAndFlush(TimeClockEventEntity.from(event));
                    return new TimeClockRegistrationResult(TimeClockRegistrationStatus.CREATED, saved.toDomain());
                });
    }
}
