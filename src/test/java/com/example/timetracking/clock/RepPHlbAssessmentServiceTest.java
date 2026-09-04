package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepPHlbAssessmentServiceTest {

    private final RepPHlbAssessmentService service = new RepPHlbAssessmentService(
            new ObservatorioNacionalNtpClient(Duration.ofSeconds(1)),
            new ObservatorioNacionalNtpEvidenceFactory()
    );

    @Test
    void composesOfficialNtpMeasurementIntoAuditableCompliantAssessment() {
        Instant t1 = Instant.parse("2026-09-04T07:30:00Z");
        NtpExchange exchange = new NtpExchange(
                t1,
                t1.plusMillis(140),
                t1.plusMillis(145),
                t1.plusMillis(45)
        );

        RepPHlbAssessmentService.Assessment assessment = service.assess(
                new ObservatorioNacionalNtpClient.Measurement("200.20.186.75", exchange)
        );

        assertEquals(Duration.ofMillis(120), assessment.evidence().offset().value());
        assertEquals(TrustedClockEvidence.Source.OBSERVATORIO_NACIONAL_PUBLIC_NTP,
                assessment.evidence().source());
        assertTrue(assessment.compliance().withinHlbTolerance());
        assertEquals(assessment.evidence().offset(), assessment.compliance().trustedClockOffset());
    }

    @Test
    void classifiesMeasuredOffsetOutsideThirtySecondsWithoutMutatingAnyPunch() {
        Instant t1 = Instant.parse("2026-09-04T07:30:00Z");
        NtpExchange exchange = new NtpExchange(
                t1,
                t1.plusSeconds(31),
                t1.plusSeconds(31),
                t1
        );

        RepPHlbAssessmentService.Assessment assessment = service.assess(
                new ObservatorioNacionalNtpClient.Measurement("200.20.186.94", exchange)
        );

        assertEquals(Duration.ofSeconds(31), assessment.evidence().offset().value());
        assertFalse(assessment.compliance().withinHlbTolerance());
    }

    @Test
    void failsClosedForMissingOrUntrustedMeasurement() {
        assertThrows(NullPointerException.class, () -> service.assess(null));

        Instant t1 = Instant.parse("2026-09-04T07:30:00Z");
        NtpExchange exchange = new NtpExchange(t1, t1, t1, t1.plusMillis(10));
        assertThrows(IllegalArgumentException.class, () -> service.assess(
                new ObservatorioNacionalNtpClient.Measurement("192.0.2.10", exchange)
        ));
    }

    @Test
    void rejectsContradictoryEvidenceAndCompliancePair() {
        Instant measuredAt = Instant.parse("2026-09-04T07:30:00Z");
        TrustedClockEvidence evidence = TrustedClockEvidence.observatorioNacionalPublicNtp(
                new TrustedClockOffset(Duration.ZERO), measuredAt, "200.20.186.75");
        RepPClockCompliance other = RepPClockCompliance.assess(
                new TrustedClockOffset(Duration.ofSeconds(31)));

        assertThrows(IllegalArgumentException.class,
                () -> new RepPHlbAssessmentService.Assessment(evidence, other));
    }
}
