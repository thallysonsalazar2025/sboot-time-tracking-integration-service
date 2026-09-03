package com.example.timetracking.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.timetracking.timesheet.TimesheetItem;
import com.example.timetracking.timesheet.TimesheetOrigin;
import com.example.timetracking.timesheet.TimesheetReadService;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TimeClockTimesheetControllerTest {

    @Mock
    private TimesheetReadService readService;

    @Test
    void forwardsTrustedIdentityCompetenceAndTimezoneToReadService() {
        TimeClockTimesheetController controller = new TimeClockTimesheetController(readService);
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        YearMonth competence = YearMonth.of(2026, 9);
        List<TimesheetItem> expected = List.of(new TimesheetItem(
                UUID.randomUUID(),
                Instant.parse("2026-09-03T12:00:00Z"),
                TimesheetOrigin.ORIGINAL,
                List.of()
        ));
        when(readService.read("tenant-a", "employee-1", competence, zone)).thenReturn(expected);

        StepVerifier.create(controller.read("tenant-a", "employee-1", "2026-09", "America/Sao_Paulo"))
                .expectNext(expected)
                .verifyComplete();

        verify(readService).read("tenant-a", "employee-1", competence, zone);
    }

    @Test
    void rejectsBlankAuthenticatedContextBeforeCallingService() {
        TimeClockTimesheetController controller = new TimeClockTimesheetController(readService);

        StepVerifier.create(controller.read("tenant-a", " ", "2026-09", "America/Sao_Paulo"))
                .expectErrorSatisfies(error -> {
                    ResponseStatusException response = assertInstanceOf(ResponseStatusException.class, error);
                    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
                })
                .verify();

        verify(readService, never()).read(
                "tenant-a",
                " ",
                YearMonth.of(2026, 9),
                ZoneId.of("America/Sao_Paulo")
        );
    }

    @Test
    void rejectsInvalidCompetenceBeforeCallingService() {
        TimeClockTimesheetController controller = new TimeClockTimesheetController(readService);

        StepVerifier.create(controller.read("tenant-a", "employee-1", "09/2026", "America/Sao_Paulo"))
                .expectErrorSatisfies(error -> {
                    ResponseStatusException response = assertInstanceOf(ResponseStatusException.class, error);
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                })
                .verify();

        verify(readService, never()).read(
                "tenant-a",
                "employee-1",
                YearMonth.of(2026, 9),
                ZoneId.of("America/Sao_Paulo")
        );
    }

    @Test
    void rejectsInvalidTimezoneBeforeCallingService() {
        TimeClockTimesheetController controller = new TimeClockTimesheetController(readService);

        StepVerifier.create(controller.read("tenant-a", "employee-1", "2026-09", "Mars/Olympus"))
                .expectErrorSatisfies(error -> {
                    ResponseStatusException response = assertInstanceOf(ResponseStatusException.class, error);
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                })
                .verify();

        verify(readService, never()).read(
                "tenant-a",
                "employee-1",
                YearMonth.of(2026, 9),
                ZoneId.of("America/Sao_Paulo")
        );
    }
}
