package com.example.timetracking.clock;

import java.time.Instant;
import java.util.Objects;

/**
 * The four timestamps of an NTP exchange:
 * t1 client send, t2 server receive, t3 server transmit, t4 client receive.
 */
public record NtpExchange(
        Instant clientSentAt,
        Instant serverReceivedAt,
        Instant serverTransmittedAt,
        Instant clientReceivedAt
) {
    public NtpExchange {
        Objects.requireNonNull(clientSentAt, "clientSentAt");
        Objects.requireNonNull(serverReceivedAt, "serverReceivedAt");
        Objects.requireNonNull(serverTransmittedAt, "serverTransmittedAt");
        Objects.requireNonNull(clientReceivedAt, "clientReceivedAt");
        if (clientReceivedAt.isBefore(clientSentAt)) {
            throw new IllegalArgumentException("clientReceivedAt must not precede clientSentAt");
        }
        if (serverTransmittedAt.isBefore(serverReceivedAt)) {
            throw new IllegalArgumentException("serverTransmittedAt must not precede serverReceivedAt");
        }
    }
}
