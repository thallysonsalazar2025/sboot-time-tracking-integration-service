package com.example.timetracking.clock;

import java.util.Objects;

/**
 * REP-P compliance signal for the clock synchronization requirement in
 * Portaria MTP 671/2021, Anexo IX, item 2.
 *
 * This assessment is audit/observability evidence only. It does not reject,
 * mutate, or delete a time punch.
 */
public record RepPClockCompliance(
        long deviceClockOffsetSeconds,
        Status status
) {
    public static final long HLB_MAX_VARIATION_SECONDS = 30L;

    public enum Status {
        WITHIN_HLB_TOLERANCE,
        OUTSIDE_HLB_TOLERANCE
    }

    public static RepPClockCompliance assess(TimeClockObservation observation) {
        Objects.requireNonNull(observation, "observation");
        long offset = observation.deviceClockOffsetSeconds();
        Status status = offset >= -HLB_MAX_VARIATION_SECONDS && offset <= HLB_MAX_VARIATION_SECONDS
                ? Status.WITHIN_HLB_TOLERANCE
                : Status.OUTSIDE_HLB_TOLERANCE;
        return new RepPClockCompliance(offset, status);
    }

    public boolean withinHlbTolerance() {
        return status == Status.WITHIN_HLB_TOLERANCE;
    }
}
