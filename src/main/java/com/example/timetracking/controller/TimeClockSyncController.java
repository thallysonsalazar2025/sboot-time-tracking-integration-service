package com.example.timetracking.controller;

import com.example.timetracking.clock.TimeClockBatchSyncService;
import com.example.timetracking.clock.TimeClockSyncItem;
import com.example.timetracking.clock.TimeClockSyncResult;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/time-clock/events")
public class TimeClockSyncController {

    static final String TRUSTED_TENANT_HEADER = "X-Authenticated-Tenant-Id";

    private final TimeClockBatchSyncService syncService;

    public TimeClockSyncController(TimeClockBatchSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<TimeClockSyncResult>> sync(
            @RequestHeader(TRUSTED_TENANT_HEADER) String trustedTenantId,
            @RequestBody List<TimeClockSyncItem> items
    ) {
        return Mono.fromCallable(() -> syncService.sync(trustedTenantId, items))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
