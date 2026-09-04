package com.example.timetracking.clock;

import java.time.Duration;
import java.util.Objects;

/**
 * Offset measured by a clock-synchronization mechanism against a trusted
 * HLB-aligned reference. This value is intentionally separate from punch
 * delivery latency such as occurredAt -> serverReceivedAt.
 */
public record TrustedClockOffset(Duration value) {
    public TrustedClockOffset {
        Objects.requireNonNull(value, "value");
    }
}
