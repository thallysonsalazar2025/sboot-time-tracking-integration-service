package com.example.timetracking.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.timetracking.domain.TimeEvent;
import com.example.timetracking.dto.SecullumResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SecullumMapperTest {

    private final SecullumMapper mapper = new SecullumMapper();

    @Test
    void shouldMapSecullumResponseToTimeEvent() {
        SecullumResponse response = new SecullumResponse("LATE", LocalDate.of(2026, 3, 10),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(23.9));

        TimeEvent event = mapper.toTimeEvent(response);

        assertEquals("LATE", event.getType());
        assertEquals(LocalDate.of(2026, 3, 10), event.getDate());
        assertEquals(BigDecimal.valueOf(1.5), event.getQuantity());
        assertEquals(BigDecimal.valueOf(23.9), event.getAmount());
    }
}
