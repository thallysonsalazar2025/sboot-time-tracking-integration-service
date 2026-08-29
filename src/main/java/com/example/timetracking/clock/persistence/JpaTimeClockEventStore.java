package com.example.timetracking.clock.persistence;

import com.example.timetracking.clock.TimeClockEvent;
import com.example.timetracking.clock.TimeClockEventStore;
import com.example.timetracking.clock.TimeClockRegistrationResult;
import com.example.timetracking.clock.TimeClockRegistrationStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class JpaTimeClockEventStore implements TimeClockEventStore {
    private final SpringDataTimeClockEventRepository repository;
    private final TimeClockEventInserter inserter;

    public JpaTimeClockEventStore(
            SpringDataTimeClockEventRepository repository,
            TimeClockEventInserter inserter
    ) {
        this.repository = repository;
        this.inserter = inserter;
    }

    @Override
    public TimeClockRegistrationResult register(TimeClockEvent event) {
        Objects.requireNonNull(event, "event");
        TimeClockEventEntity existing = find(event);
        if (existing != null) {
            return result(TimeClockRegistrationStatus.EXISTING, existing);
        }

        try {
            return result(TimeClockRegistrationStatus.CREATED, inserter.insert(event));
        } catch (DataIntegrityViolationException race) {
            TimeClockEventEntity concurrent = find(event);
            if (concurrent == null) {
                throw race;
            }
            return result(TimeClockRegistrationStatus.EXISTING, concurrent);
        }
    }

    private TimeClockEventEntity find(TimeClockEvent event) {
        return repository.findByTenantIdAndEmployeeIdAndClientEventId(
                        event.tenantId(),
                        event.employeeId(),
                        event.clientEventId()
                )
                .orElse(null);
    }

    private TimeClockRegistrationResult result(
            TimeClockRegistrationStatus status,
            TimeClockEventEntity entity
    ) {
        return new TimeClockRegistrationResult(status, entity.toDomain());
    }
}
