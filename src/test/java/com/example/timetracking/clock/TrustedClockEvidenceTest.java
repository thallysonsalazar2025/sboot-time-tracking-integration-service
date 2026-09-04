package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrustedClockEvidenceTest {

    private final Instant measuredAt = Instant.parse("2026-09-04T03:40:00Z");

    @Test
    void acceptsOfficialObservatorioNacionalPublicNtpEndpoints() {
        TrustedClockEvidence first = TrustedClockEvidence.observatorioNacionalPublicNtp(
                new TrustedClockOffset(Duration.ofMillis(125)),
                measuredAt,
                "200.20.186.75"
        );
        TrustedClockEvidence second = TrustedClockEvidence.observatorioNacionalPublicNtp(
                new TrustedClockOffset(Duration.ofMillis(-80)),
                measuredAt,
                "200.20.186.94"
        );

        assertEquals(TrustedClockEvidence.Source.OBSERVATORIO_NACIONAL_PUBLIC_NTP, first.source());
        assertEquals("200.20.186.75", first.referenceId());
        assertEquals(measuredAt, first.measuredAt());
        assertEquals("200.20.186.94", second.referenceId());
    }

    @Test
    void rejectsUnknownEndpointClaimedAsObservatorioNacionalPublicNtp() {
        assertThrows(IllegalArgumentException.class, () ->
                TrustedClockEvidence.observatorioNacionalPublicNtp(
                        new TrustedClockOffset(Duration.ZERO),
                        measuredAt,
                        "192.0.2.10"
                ));
    }

    @Test
    void rejectsBlankReferenceForCertifiedSyncEvidence() {
        assertThrows(IllegalArgumentException.class, () ->
                new TrustedClockEvidence(
                        new TrustedClockOffset(Duration.ZERO),
                        measuredAt,
                        TrustedClockEvidence.Source.HLB_CERTIFIED_SYNC_SERVICE,
                        " "
                ));
    }

    @Test
    void rejectsMissingEvidenceFields() {
        TrustedClockOffset offset = new TrustedClockOffset(Duration.ZERO);

        assertThrows(NullPointerException.class, () ->
                new TrustedClockEvidence(null, measuredAt,
                        TrustedClockEvidence.Source.HLB_CERTIFIED_SYNC_SERVICE, "provider"));
        assertThrows(NullPointerException.class, () ->
                new TrustedClockEvidence(offset, null,
                        TrustedClockEvidence.Source.HLB_CERTIFIED_SYNC_SERVICE, "provider"));
        assertThrows(NullPointerException.class, () ->
                new TrustedClockEvidence(offset, measuredAt, null, "provider"));
    }
}
