package com.example.timetracking.timesheet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaTimesheetReadRepository.class, TimesheetReadService.class})
@Testcontainers
class TimesheetReadPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TimesheetReadService service;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("delete from time_clock_adjustment");
        jdbcTemplate.update("delete from time_clock_event");
    }

    @Test
    void isolatesTenantAndEmployeeAndComposesOnlyApprovedAdjustments() {
        UUID tenantAEvent = UUID.randomUUID();
        UUID tenantBEvent = UUID.randomUUID();
        UUID otherEmployeeEvent = UUID.randomUUID();
        UUID approvedAdjustment = UUID.randomUUID();
        UUID pendingAdjustment = UUID.randomUUID();

        insertEvent(UUID.randomUUID(), "tenant-a", "employee-1", tenantAEvent, "2026-08-12T12:00:00Z");
        insertEvent(UUID.randomUUID(), "tenant-b", "employee-1", tenantBEvent, "2026-08-12T12:00:00Z");
        insertEvent(UUID.randomUUID(), "tenant-a", "employee-2", otherEmployeeEvent, "2026-08-12T12:00:00Z");

        insertAdjustment(approvedAdjustment, "tenant-a", "employee-1", tenantAEvent, "APPROVED");
        insertAdjustment(pendingAdjustment, "tenant-a", "employee-1", tenantAEvent, "PENDING_APPROVAL");

        List<TimesheetItem> result = service.read(
                "tenant-a",
                "employee-1",
                YearMonth.of(2026, 8),
                ZoneId.of("America/Sao_Paulo")
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().clientEventId()).isEqualTo(tenantAEvent);
        assertThat(result.getFirst().approvedAdjustmentIds()).containsExactly(approvedAdjustment);
        assertThat(result).noneMatch(item -> item.clientEventId().equals(tenantBEvent));
        assertThat(result).noneMatch(item -> item.clientEventId().equals(otherEmployeeEvent));
    }

    @Test
    void excludesEventsOutsideBusinessZoneCompetenceWindow() {
        UUID julyInSaoPaulo = UUID.randomUUID();
        UUID augustInSaoPaulo = UUID.randomUUID();

        insertEvent(UUID.randomUUID(), "tenant-a", "employee-1", julyInSaoPaulo, "2026-08-01T02:59:59Z");
        insertEvent(UUID.randomUUID(), "tenant-a", "employee-1", augustInSaoPaulo, "2026-08-01T03:00:00Z");

        List<TimesheetItem> result = service.read(
                "tenant-a",
                "employee-1",
                YearMonth.of(2026, 8),
                ZoneId.of("America/Sao_Paulo")
        );

        assertThat(result).extracting(TimesheetItem::clientEventId)
                .containsExactly(augustInSaoPaulo);
    }

    private void insertEvent(UUID id, String tenantId, String employeeId, UUID clientEventId, String occurredAt) {
        jdbcTemplate.update("""
                insert into time_clock_event(id, tenant_id, employee_id, client_event_id, occurred_at)
                values (?, ?, ?, ?, cast(? as timestamptz))
                """, id, tenantId, employeeId, clientEventId, occurredAt);
    }

    private void insertAdjustment(UUID id, String tenantId, String employeeId, UUID originalClientEventId, String status) {
        jdbcTemplate.update("""
                insert into time_clock_adjustment(
                    id, tenant_id, employee_id, original_client_event_id, justification,
                    requested_by, requested_at, status, decided_by, decided_at
                ) values (?, ?, ?, ?, 'review', 'manager', cast('2026-08-12T13:00:00Z' as timestamptz), ?,
                          case when ? = 'APPROVED' then 'manager' else null end,
                          case when ? = 'APPROVED' then cast('2026-08-12T14:00:00Z' as timestamptz) else null end)
                """, id, tenantId, employeeId, originalClientEventId, status, status, status);
    }
}
