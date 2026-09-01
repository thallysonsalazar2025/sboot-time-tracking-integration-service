package com.example.timetracking.controller;

import com.example.timetracking.clock.TimeClockBatchSyncService;
import com.example.timetracking.clock.TimeClockSyncItem;
import com.example.timetracking.clock.TimeClockSyncResult;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/time-clock/events")
public class TimeClockSyncController {

    static final String TRUSTED_TENANT_HEADER = "X-Authenticated-Tenant-Id";
    static final String TRUSTED_EMPLOYEE_HEADER = "X-Authenticated-Employee-Id";

    private final TimeClockBatchSyncService syncService;

    public TimeClockSyncController(TimeClockBatchSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<TimeClockSyncResult>> sync(
            @RequestHeader(TRUSTED_TENANT_HEADER) String trustedTenantId,
            @RequestHeader(TRUSTED_EMPLOYEE_HEADER) String trustedEmployeeId,
            @RequestBody List<TimeClockSyncItem> items
    ) {
        if (trustedEmployeeId.isBlank() || items.stream().anyMatch(item -> !trustedEmployeeId.equals(item.employeeId()))) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated employee does not own all time-clock events"
            ));
        }

        return Mono.fromCallable(() -> syncService.sync(trustedTenantId, items))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
