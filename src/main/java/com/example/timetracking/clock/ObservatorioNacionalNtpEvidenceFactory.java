package com.example.timetracking.clock;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Converts a validated NTP exchange with an official Observatorio Nacional endpoint
 * into auditable HLB clock evidence. Network I/O stays outside this class.
 */
public final class ObservatorioNacionalNtpEvidenceFactory {

    public TrustedClockEvidence fromExchange(String serverAddress, NtpExchange exchange) {
        Objects.requireNonNull(exchange, "exchange");

        Duration firstLeg = Duration.between(exchange.clientSentAt(), exchange.serverReceivedAt());
        Duration secondLeg = Duration.between(exchange.clientReceivedAt(), exchange.serverTransmittedAt());
        Duration offset = firstLeg.plus(secondLeg).dividedBy(2);
        Instant measuredAt = midpoint(exchange.clientSentAt(), exchange.clientReceivedAt());

        return TrustedClockEvidence.observatorioNacionalPublicNtp(
                new TrustedClockOffset(offset),
                measuredAt,
                serverAddress
        );
    }

    private Instant midpoint(Instant start, Instant end) {
        Duration elapsed = Duration.between(start, end);
        return start.plus(elapsed.dividedBy(2));
    }
}
