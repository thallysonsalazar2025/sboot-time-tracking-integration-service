package com.example.timetracking.mapper;

import com.example.timetracking.domain.TimeEvent;
import com.example.timetracking.dto.SecullumResponse;
import org.springframework.stereotype.Component;

@Component
public class SecullumMapper {

    public TimeEvent toTimeEvent(SecullumResponse response) {
        return new TimeEvent(
                response.getEventType(),
                response.getEventDate(),
                response.getHours(),
                response.getValue());
    }
}
