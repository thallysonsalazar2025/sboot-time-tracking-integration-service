package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservatorioNacionalNtpEvidenceFactoryTest {

    private final ObservatorioNacionalNtpEvidenceFactory factory = new ObservatorioNacionalNtpEvidenceFactory();
    private final Instant t1 = Instant.parse("2026-09-04T04:00:00Z");

    @Test
    void derivesPositiveOffsetWithoutUsingPunchDeliveryLatency() {
        NtpExchange exchange = new NtpExchange(
                t1,
                t1.plusMillis(140),
                t1.plusMillis(145),
                t1.plusMillis(45)
        );

        TrustedClockEvidence evidence = factory.fromExchange("200.20.186.75", exchange);

        assertEquals(Duration.ofMillis(120), evidence.offset().value());
        assertEquals(t1.plusMillis(22).plusNanos(500_000), evidence.measuredAt());
        assertEquals(TrustedClockEvidence.Source.OBSERVATORIO_NACIONAL_PUBLIC_NTP, evidence.source());
    }

    @Test
    void derivesNegativeOffset() {
        NtpExchange exchange = new NtpExchange(
                t1,
                t1.minusMillis(70),
                t1.minusMillis(65),
                t1.plusMillis(25)
        );

        TrustedClockEvidence evidence = factory.fromExchange("200.20.186.94", exchange);

        assertEquals(Duration.ofMillis(-80), evidence.offset().value());
    }

    @Test
    void rejectsUnofficialEndpointAndImpossibleExchangeOrdering() {
        NtpExchange valid = new NtpExchange(t1, t1, t1, t1.plusMillis(1));
        assertThrows(IllegalArgumentException.class,
                () -> factory.fromExchange("192.0.2.1", valid));
        assertThrows(IllegalArgumentException.class,
                () -> new NtpExchange(t1, t1, t1, t1.minusMillis(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new NtpExchange(t1, t1.plusMillis(2), t1.plusMillis(1), t1.plusMillis(3)));
    }
}
