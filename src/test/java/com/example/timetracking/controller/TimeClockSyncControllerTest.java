package com.example.timetracking.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.timetracking.clock.TimeClockBatchSyncService;
import com.example.timetracking.clock.TimeClockRegistrationStatus;
import com.example.timetracking.clock.TimeClockSyncItem;
import com.example.timetracking.clock.TimeClockSyncResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TimeClockSyncControllerTest {

    @Mock
    private TimeClockBatchSyncService syncService;

    @Test
    void forwardsOnlyTrustedTenantHeaderToBatchService() {
        TimeClockSyncController controller = new TimeClockSyncController(syncService);
        UUID eventId = UUID.randomUUID();
        List<TimeClockSyncItem> items = List.of(
                new TimeClockSyncItem("employee-1", eventId, Instant.parse("2026-08-30T04:30:00Z"))
        );
        List<TimeClockSyncResult> expected = List.of(
                TimeClockSyncResult.accepted(eventId, TimeClockRegistrationStatus.CREATED)
        );
        when(syncService.sync("tenant-a", items)).thenReturn(expected);

        StepVerifier.create(controller.sync("tenant-a", items))
                .expectNext(expected)
                .verifyComplete();

        verify(syncService).sync("tenant-a", items);
    }
}
