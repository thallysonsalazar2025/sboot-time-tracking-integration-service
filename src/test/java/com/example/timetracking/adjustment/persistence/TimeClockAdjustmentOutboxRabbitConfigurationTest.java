package com.example.timetracking.adjustment.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;

class TimeClockAdjustmentOutboxRabbitConfigurationTest {

    @Test
    void serializesOutboxMessageAsJson() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var converter = new TimeClockAdjustmentOutboxRabbitConfiguration()
                .timeClockAdjustmentMessageConverter(objectMapper);
        var payload = new TimeClockAdjustmentOutboxDispatcher.OutboxMessage(
                UUID.randomUUID(),
                "tenant-a",
                UUID.randomUUID(),
                "TIME_CLOCK_ADJUSTMENT_APPROVED",
                "employee-1",
                UUID.randomUUID(),
                "manager-1",
                Instant.parse("2026-08-31T12:00:00Z"));

        var message = converter.toMessage(payload, new MessageProperties());

        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(new String(message.getBody())).contains("tenant-a", "TIME_CLOCK_ADJUSTMENT_APPROVED");
    }
}
