package com.example.timetracking.clock;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * Orchestrates trusted HLB measurement into a REP-P compliance assessment.
 *
 * This service never reads, rejects, mutates or deletes a time punch. Network
 * failures propagate as IOException so callers cannot fabricate compliance
 * evidence when the trusted time source is unavailable.
 */
public final class RepPHlbAssessmentService {

    private final ObservatorioNacionalNtpClient ntpClient;
    private final ObservatorioNacionalNtpEvidenceFactory evidenceFactory;
    private final RepPHlbAssessmentObserver observer;

    public RepPHlbAssessmentService(Duration timeout) {
        this(
                new ObservatorioNacionalNtpClient(timeout),
                new ObservatorioNacionalNtpEvidenceFactory(),
                RepPHlbAssessmentObserver.noop()
        );
    }

    RepPHlbAssessmentService(
            ObservatorioNacionalNtpClient ntpClient,
            ObservatorioNacionalNtpEvidenceFactory evidenceFactory
    ) {
        this(ntpClient, evidenceFactory, RepPHlbAssessmentObserver.noop());
    }

    RepPHlbAssessmentService(
            ObservatorioNacionalNtpClient ntpClient,
            ObservatorioNacionalNtpEvidenceFactory evidenceFactory,
            RepPHlbAssessmentObserver observer
    ) {
        this.ntpClient = Objects.requireNonNull(ntpClient, "ntpClient");
        this.evidenceFactory = Objects.requireNonNull(evidenceFactory, "evidenceFactory");
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public Assessment measureAndAssess() throws IOException {
        try {
            return assess(ntpClient.measure());
        } catch (IOException ex) {
            observer.assessmentFailed(RepPHlbAssessmentObserver.Failure.TRUSTED_TIME_UNAVAILABLE);
            throw ex;
        }
    }

    Assessment assess(ObservatorioNacionalNtpClient.Measurement measurement) {
        Objects.requireNonNull(measurement, "measurement");
        TrustedClockEvidence evidence = evidenceFactory.fromExchange(
                measurement.serverAddress(),
                measurement.exchange()
        );
        RepPClockCompliance compliance = RepPClockCompliance.assess(evidence.offset());
        Assessment assessment = new Assessment(evidence, compliance);
        observer.assessmentCompleted(RepPHlbAssessmentObserver.AssessmentSignal.from(assessment));
        return assessment;
    }

    public record Assessment(
            TrustedClockEvidence evidence,
            RepPClockCompliance compliance
    ) {
        public Assessment {
            Objects.requireNonNull(evidence, "evidence");
            Objects.requireNonNull(compliance, "compliance");
            if (!evidence.offset().equals(compliance.trustedClockOffset())) {
                throw new IllegalArgumentException("compliance must be derived from the supplied evidence");
            }
        }
    }
}
