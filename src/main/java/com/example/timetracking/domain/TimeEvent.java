package com.example.timetracking.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TimeEvent {

    private String type;
    private LocalDate date;
    private BigDecimal quantity;
    private BigDecimal amount;

    public TimeEvent() {
    }

    public TimeEvent(String type, LocalDate date, BigDecimal quantity, BigDecimal amount) {
        this.type = type;
        this.date = date;
        this.quantity = quantity;
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
