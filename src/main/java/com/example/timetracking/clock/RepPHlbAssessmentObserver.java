package com.example.timetracking.clock;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Low-cardinality observability port for REP-P/HLB assessments.
 *
 * Deliberately excludes tenant, employee, punch identifiers and other PII.
 * Adapters may translate these signals into metrics/audit records without
 * coupling observability to punch acceptance.
 */
public interface RepPHlbAssessmentObserver {

    void assessmentCompleted(AssessmentSignal signal);

    void assessmentFailed(Failure failure);

    static RepPHlbAssessmentObserver noop() {
        return NoopHolder.INSTANCE;
    }

    enum Failure {
        TRUSTED_TIME_UNAVAILABLE
    }

    record AssessmentSignal(
            Instant measuredAt,
            TrustedClockEvidence.Source source,
            Duration signedOffset,
            RepPClockCompliance.Status status
    ) {
        public AssessmentSignal {
            Objects.requireNonNull(measuredAt, "measuredAt");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(signedOffset, "signedOffset");
            Objects.requireNonNull(status, "status");
        }

        static AssessmentSignal from(RepPHlbAssessmentService.Assessment assessment) {
            Objects.requireNonNull(assessment, "assessment");
            return new AssessmentSignal(
                    assessment.evidence().measuredAt(),
                    assessment.evidence().source(),
                    assessment.evidence().offset().value(),
                    assessment.compliance().status()
            );
        }
    }

    final class NoopHolder {
        private static final RepPHlbAssessmentObserver INSTANCE = new RepPHlbAssessmentObserver() {
            @Override
            public void assessmentCompleted(AssessmentSignal signal) {
                // intentionally empty
            }

            @Override
            public void assessmentFailed(Failure failure) {
                // intentionally empty
            }
        };

        private NoopHolder() {
        }
    }
}
