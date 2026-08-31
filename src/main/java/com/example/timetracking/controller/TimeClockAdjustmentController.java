package com.example.timetracking.controller;

import com.example.timetracking.adjustment.TimeClockAdjustment;
import com.example.timetracking.adjustment.TimeClockAdjustmentService;
import com.example.timetracking.adjustment.TimeClockAdjustmentStatus;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/time-clock/adjustments")
public class TimeClockAdjustmentController {

    static final String TRUSTED_TENANT_HEADER = "X-Authenticated-Tenant-Id";
    static final String TRUSTED_ACTOR_HEADER = "X-Authenticated-Actor-Id";

    private final TimeClockAdjustmentService service;

    public TimeClockAdjustmentController(TimeClockAdjustmentService service) {
        this.service = service;
    }

    @PatchMapping(value = "/{adjustmentId}/decision", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DecisionResponse> decide(
            @RequestHeader(TRUSTED_TENANT_HEADER) String tenantId,
            @RequestHeader(TRUSTED_ACTOR_HEADER) String actorId,
            @PathVariable UUID adjustmentId,
            @RequestBody DecisionRequest request
    ) {
        return Mono.fromCallable(() -> service.decide(tenantId, adjustmentId, request.decision(), actorId))
                .map(DecisionResponse::from)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public record DecisionRequest(TimeClockAdjustmentStatus decision) {
        public DecisionRequest {
            if (decision == null || decision == TimeClockAdjustmentStatus.PENDING_APPROVAL) {
                throw new IllegalArgumentException("decision must be terminal");
            }
        }
    }

    public record DecisionResponse(
            UUID adjustmentId,
            String tenantId,
            String employeeId,
            TimeClockAdjustmentStatus status,
            String decidedBy,
            String decidedAt
    ) {
        static DecisionResponse from(TimeClockAdjustment adjustment) {
            return new DecisionResponse(
                    adjustment.id(),
                    adjustment.tenantId(),
                    adjustment.employeeId(),
                    adjustment.status(),
                    adjustment.decidedBy(),
                    adjustment.decidedAt().toString());
        }
    }
}
