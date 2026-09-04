package com.example.timetracking.clock;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Audit evidence that a clock offset was measured against an HLB-aligned
 * reference. Delivery latency from a punch is deliberately not accepted here.
 */
public record TrustedClockEvidence(
        TrustedClockOffset offset,
        Instant measuredAt,
        Source source,
        String referenceId
) {
    private static final Set<String> OBSERVATORIO_NACIONAL_PUBLIC_NTP = Set.of(
            "200.20.186.75",
            "200.20.186.94"
    );

    public enum Source {
        OBSERVATORIO_NACIONAL_PUBLIC_NTP,
        HLB_CERTIFIED_SYNC_SERVICE
    }

    public TrustedClockEvidence {
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(measuredAt, "measuredAt");
        Objects.requireNonNull(source, "source");
        referenceId = requireText(referenceId, "referenceId");

        if (source == Source.OBSERVATORIO_NACIONAL_PUBLIC_NTP
                && !OBSERVATORIO_NACIONAL_PUBLIC_NTP.contains(referenceId)) {
            throw new IllegalArgumentException(
                    "referenceId is not an official Observatorio Nacional public NTP endpoint");
        }
    }

    public static TrustedClockEvidence observatorioNacionalPublicNtp(
            TrustedClockOffset offset,
            Instant measuredAt,
            String serverAddress
    ) {
        return new TrustedClockEvidence(
                offset,
                measuredAt,
                Source.OBSERVATORIO_NACIONAL_PUBLIC_NTP,
                serverAddress
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
