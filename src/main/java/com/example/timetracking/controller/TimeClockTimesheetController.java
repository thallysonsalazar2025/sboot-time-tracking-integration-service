package com.example.timetracking.controller;

import com.example.timetracking.timesheet.TimesheetItem;
import com.example.timetracking.timesheet.TimesheetReadService;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/time-clock/timesheet")
public class TimeClockTimesheetController {

    static final String TRUSTED_TENANT_HEADER = "X-Authenticated-Tenant-Id";
    static final String TRUSTED_EMPLOYEE_HEADER = "X-Authenticated-Employee-Id";

    private final TimesheetReadService readService;

    public TimeClockTimesheetController(TimesheetReadService readService) {
        this.readService = readService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<TimesheetItem>> read(
            @RequestHeader(TRUSTED_TENANT_HEADER) String trustedTenantId,
            @RequestHeader(TRUSTED_EMPLOYEE_HEADER) String trustedEmployeeId,
            @RequestParam String competence,
            @RequestParam String timezone
    ) {
        if (trustedTenantId.isBlank() || trustedEmployeeId.isBlank()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated time-clock context is required"
            ));
        }

        final YearMonth parsedCompetence;
        final ZoneId businessZone;
        try {
            parsedCompetence = YearMonth.parse(competence);
            businessZone = ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid competence or timezone",
                    exception
            ));
        }

        return Mono.fromCallable(() -> readService.read(
                        trustedTenantId,
                        trustedEmployeeId,
                        parsedCompetence,
                        businessZone
                ))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
