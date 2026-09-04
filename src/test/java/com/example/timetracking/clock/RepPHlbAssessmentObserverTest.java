package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepPHlbAssessmentObserverTest {

    @Test
    void emitsLowCardinalitySignalWithoutTenantEmployeeOrPunchIdentity() {
        RecordingObserver observer = new RecordingObserver();
        RepPHlbAssessmentService service = new RepPHlbAssessmentService(
                new ObservatorioNacionalNtpClient(Duration.ofSeconds(1)),
                new ObservatorioNacionalNtpEvidenceFactory(),
                observer
        );

        Instant t1 = Instant.parse("2026-09-04T08:30:00Z");
        NtpExchange exchange = new NtpExchange(
                t1,
                t1.plusMillis(140),
                t1.plusMillis(145),
                t1.plusMillis(45)
        );

        RepPHlbAssessmentService.Assessment assessment = service.assess(
                new ObservatorioNacionalNtpClient.Measurement("200.20.186.75", exchange)
        );

        assertEquals(1, observer.completed.size());
        RepPHlbAssessmentObserver.AssessmentSignal signal = observer.completed.getFirst();
        assertEquals(assessment.evidence().measuredAt(), signal.measuredAt());
        assertEquals(TrustedClockEvidence.Source.OBSERVATORIO_NACIONAL_PUBLIC_NTP, signal.source());
        assertEquals(Duration.ofMillis(120), signal.signedOffset());
        assertEquals(RepPClockCompliance.Status.WITHIN_HLB_TOLERANCE, signal.status());
        assertTrue(observer.failures.isEmpty());
    }

    @Test
    void outsideToleranceIsObservedButAssessmentStillExists() {
        RecordingObserver observer = new RecordingObserver();
        RepPHlbAssessmentService service = new RepPHlbAssessmentService(
                new ObservatorioNacionalNtpClient(Duration.ofSeconds(1)),
                new ObservatorioNacionalNtpEvidenceFactory(),
                observer
        );

        Instant t1 = Instant.parse("2026-09-04T08:30:00Z");
        NtpExchange exchange = new NtpExchange(t1, t1.plusSeconds(31), t1.plusSeconds(31), t1);

        RepPHlbAssessmentService.Assessment assessment = service.assess(
                new ObservatorioNacionalNtpClient.Measurement("200.20.186.94", exchange)
        );

        assertEquals(RepPClockCompliance.Status.OUTSIDE_HLB_TOLERANCE,
                observer.completed.getFirst().status());
        assertEquals(Duration.ofSeconds(31), assessment.evidence().offset().value());
    }

    private static final class RecordingObserver implements RepPHlbAssessmentObserver {
        private final List<AssessmentSignal> completed = new ArrayList<>();
        private final List<Failure> failures = new ArrayList<>();

        @Override
        public void assessmentCompleted(AssessmentSignal signal) {
            completed.add(signal);
        }

        @Override
        public void assessmentFailed(Failure failure) {
            failures.add(failure);
        }
    }
}
