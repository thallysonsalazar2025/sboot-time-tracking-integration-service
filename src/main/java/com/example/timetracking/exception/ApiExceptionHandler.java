package com.example.timetracking.exception;

import com.example.timetracking.adjustment.TimeClockAdjustmentDecisionConflictException;
import com.example.timetracking.adjustment.TimeClockAdjustmentNotFoundException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IntegrationConfigNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleConfigNotFound(IntegrationConfigNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TimeClockAdjustmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAdjustmentNotFound(TimeClockAdjustmentNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TimeClockAdjustmentDecisionConflictException.class)
    public ResponseEntity<Map<String, Object>> handleAdjustmentConflict(TimeClockAdjustmentDecisionConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExternalIntegrationException.class)
    public ResponseEntity<Map<String, Object>> handleExternalError(ExternalIntegrationException ex) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
