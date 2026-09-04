package com.example.timetracking.clock;

import java.time.Duration;
import java.util.Objects;

/**
 * REP-P compliance signal for the clock synchronization requirement in
 * Portaria MTP 671/2021, Anexo IX, item 2.
 *
 * This assessment is audit/observability evidence only. It does not reject,
 * mutate, or delete a time punch. Punch delivery latency must not be used as
 * evidence of HLB synchronization.
 */
public record RepPClockCompliance(TrustedClockOffset trustedClockOffset) {
    public static final Duration HLB_MAX_VARIATION = Duration.ofSeconds(30);

    public enum Status {
        WITHIN_HLB_TOLERANCE,
        OUTSIDE_HLB_TOLERANCE
    }

    public RepPClockCompliance {
        Objects.requireNonNull(trustedClockOffset, "trustedClockOffset");
    }

    public static RepPClockCompliance assess(TrustedClockOffset trustedClockOffset) {
        return new RepPClockCompliance(trustedClockOffset);
    }

    public Status status() {
        Duration offset = trustedClockOffset.value();
        return offset.compareTo(HLB_MAX_VARIATION.negated()) >= 0
                && offset.compareTo(HLB_MAX_VARIATION) <= 0
                ? Status.WITHIN_HLB_TOLERANCE
                : Status.OUTSIDE_HLB_TOLERANCE;
    }

    public boolean withinHlbTolerance() {
        return status() == Status.WITHIN_HLB_TOLERANCE;
    }
}
