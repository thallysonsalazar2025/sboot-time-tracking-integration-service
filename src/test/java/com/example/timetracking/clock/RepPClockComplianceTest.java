package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepPClockComplianceTest {

    private final Instant serverReceivedAt = Instant.parse("2026-09-03T20:00:00Z");

    @Test
    void acceptsOffsetsWithinThirtySecondHlbBoundary() {
        assertTrue(assess(29).withinHlbTolerance());
        assertTrue(assess(30).withinHlbTolerance());
        assertTrue(assess(-30).withinHlbTolerance());
    }

    @Test
    void flagsOffsetsOutsideThirtySecondHlbBoundaryWithoutRejectingPunch() {
        assertFalse(assess(31).withinHlbTolerance());
        assertFalse(assess(-31).withinHlbTolerance());
    }

    @Test
    void requiresObservation() {
        assertThrows(NullPointerException.class, () -> RepPClockCompliance.assess(null));
    }

    private RepPClockCompliance assess(long offsetSeconds) {
        return RepPClockCompliance.assess(TimeClockObservation.between(
                serverReceivedAt.plusSeconds(offsetSeconds),
                serverReceivedAt
        ));
    }
}
