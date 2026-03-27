package com.example.timetracking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SecullumResponse {

    private String eventType;
    private LocalDate eventDate;
    private BigDecimal hours;
    private BigDecimal value;

    public SecullumResponse() {
    }

    public SecullumResponse(String eventType, LocalDate eventDate, BigDecimal hours, BigDecimal value) {
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.hours = hours;
        this.value = value;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
