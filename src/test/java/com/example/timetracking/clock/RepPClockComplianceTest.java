package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepPClockComplianceTest {

    @Test
    void acceptsOffsetsWithinThirtySecondHlbBoundary() {
        assertTrue(assess(Duration.ofSeconds(29)).withinHlbTolerance());
        assertTrue(assess(Duration.ofSeconds(30)).withinHlbTolerance());
        assertTrue(assess(Duration.ofSeconds(-30)).withinHlbTolerance());
    }

    @Test
    void preservesSubsecondPrecisionAtHlbBoundary() {
        assertFalse(assess(Duration.ofSeconds(30).plusMillis(1)).withinHlbTolerance());
        assertFalse(assess(Duration.ofSeconds(-30).minusMillis(1)).withinHlbTolerance());
    }

    @Test
    void flagsOffsetsOutsideThirtySecondHlbBoundaryWithoutRejectingPunch() {
        assertFalse(assess(Duration.ofSeconds(31)).withinHlbTolerance());
        assertFalse(assess(Duration.ofSeconds(-31)).withinHlbTolerance());
    }

    @Test
    void derivesStatusFromOffsetSoContradictoryStateCannotBeConstructed() {
        RepPClockCompliance compliance = assess(Duration.ofSeconds(31));

        assertEquals(RepPClockCompliance.Status.OUTSIDE_HLB_TOLERANCE, compliance.status());
    }

    @Test
    void requiresTrustedClockObservation() {
        assertThrows(NullPointerException.class, () -> RepPClockCompliance.assess(null));
        assertThrows(NullPointerException.class, () -> new TrustedClockOffset(null));
    }

    private RepPClockCompliance assess(Duration offset) {
        return RepPClockCompliance.assess(new TrustedClockOffset(offset));
    }
}
