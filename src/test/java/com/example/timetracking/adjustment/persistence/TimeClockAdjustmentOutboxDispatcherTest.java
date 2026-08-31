package com.example.timetracking.adjustment.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class TimeClockAdjustmentOutboxDispatcherTest {

    @Test
    void marksPublishedOnlyAfterBrokerConfirm() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        var message = message();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenReturn(List.of(message));
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation -> {
            RabbitOperations.OperationsCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(operations);
        });

        var dispatcher = new TimeClockAdjustmentOutboxDispatcher(
                jdbcTemplate, rabbitTemplate, "exchange", "routing", 50);

        dispatcher.dispatchPending();

        verify(operations).convertAndSend("exchange", "routing", message);
        verify(operations).waitForConfirmsOrDie(any());
        verify(jdbcTemplate).update(
                eq("update time_clock_adjustment_outbox set published_at = ?, last_error = null where id = ? and published_at is null"),
                any(Instant.class), eq(message.id()));
    }

    @Test
    void recordsAttemptAndDoesNotPublishTimestampWhenBrokerFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        var message = message();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenReturn(List.of(message));
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation -> {
            RabbitOperations.OperationsCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(operations);
        });
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(operations).waitForConfirmsOrDie(any());

        var dispatcher = new TimeClockAdjustmentOutboxDispatcher(
                jdbcTemplate, rabbitTemplate, "exchange", "routing", 50);

        dispatcher.dispatchPending();

        verify(jdbcTemplate).update(
                "update time_clock_adjustment_outbox set attempt_count = attempt_count + 1, last_error = ? where id = ? and published_at is null",
                "broker unavailable", message.id());
        verify(jdbcTemplate, never()).update(
                eq("update time_clock_adjustment_outbox set published_at = ?, last_error = null where id = ? and published_at is null"),
                any(), any());
    }

    private TimeClockAdjustmentOutboxDispatcher.OutboxMessage message() {
        return new TimeClockAdjustmentOutboxDispatcher.OutboxMessage(
                UUID.randomUUID(),
                "tenant-a",
                UUID.randomUUID(),
                "TIME_CLOCK_ADJUSTMENT_APPROVED",
                "employee-1",
                UUID.randomUUID(),
                "manager-1",
                Instant.parse("2026-08-31T12:00:00Z"));
    }
}
