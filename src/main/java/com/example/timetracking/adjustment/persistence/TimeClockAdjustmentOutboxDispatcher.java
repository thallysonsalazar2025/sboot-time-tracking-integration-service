package com.example.timetracking.adjustment.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TimeClockAdjustmentOutboxDispatcher {
    private static final String FIND_PENDING = """
            select id, tenant_id, adjustment_id, event_type, employee_id,
                   original_client_event_id, decided_by, decided_at
              from time_clock_adjustment_outbox
             where published_at is null
             order by created_at
             limit ?
             for update skip locked
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final int batchSize;

    public TimeClockAdjustmentOutboxDispatcher(
            JdbcTemplate jdbcTemplate,
            RabbitTemplate rabbitTemplate,
            @Value("${time-clock.adjustment.outbox.exchange:payroll.time-clock}") String exchange,
            @Value("${time-clock.adjustment.outbox.routing-key:adjustment.approved}") String routingKey,
            @Value("${time-clock.adjustment.outbox.batch-size:50}") int batchSize) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${time-clock.adjustment.outbox.fixed-delay-ms:5000}")
    @Transactional
    public void dispatchPending() {
        List<OutboxMessage> messages = jdbcTemplate.query(
                FIND_PENDING,
                (rs, rowNum) -> new OutboxMessage(
                        rs.getObject("id", UUID.class),
                        rs.getString("tenant_id"),
                        rs.getObject("adjustment_id", UUID.class),
                        rs.getString("event_type"),
                        rs.getString("employee_id"),
                        rs.getObject("original_client_event_id", UUID.class),
                        rs.getString("decided_by"),
                        rs.getTimestamp("decided_at").toInstant()),
                batchSize);

        for (OutboxMessage message : messages) {
            dispatch(message);
        }
    }

    private void dispatch(OutboxMessage message) {
        try {
            rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(exchange, routingKey, message);
                operations.waitForConfirmsOrDie(Duration.ofSeconds(5));
                return null;
            });
            jdbcTemplate.update(
                    "update time_clock_adjustment_outbox set published_at = ?, last_error = null where id = ? and published_at is null",
                    Instant.now(), message.id());
        } catch (RuntimeException ex) {
            jdbcTemplate.update(
                    "update time_clock_adjustment_outbox set attempt_count = attempt_count + 1, last_error = ? where id = ? and published_at is null",
                    abbreviate(ex.getMessage()), message.id());
        }
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "RabbitMQ publish failed";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    public record OutboxMessage(
            UUID id,
            String tenantId,
            UUID adjustmentId,
            String eventType,
            String employeeId,
            UUID originalClientEventId,
            String decidedBy,
            Instant decidedAt) {
    }
}
