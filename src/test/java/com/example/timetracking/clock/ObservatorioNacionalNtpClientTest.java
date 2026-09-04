package com.example.timetracking.clock;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservatorioNacionalNtpClientTest {

    @Test
    void exchangesWithUdpServerAndCapturesFourTimestamps() throws Exception {
        try (DatagramSocket server = new DatagramSocket(0, InetAddress.getLoopbackAddress());
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> responder = executor.submit(() -> respondOnce(server));

            ObservatorioNacionalNtpClient client = new ObservatorioNacionalNtpClient(
                    List.of(InetAddress.getLoopbackAddress().getHostAddress()),
                    server.getLocalPort(),
                    Duration.ofSeconds(2),
                    Clock.systemUTC()
            );

            ObservatorioNacionalNtpClient.Measurement measurement = client.measure();

            assertEquals(InetAddress.getLoopbackAddress().getHostAddress(), measurement.serverAddress());
            assertTrue(!measurement.exchange().clientReceivedAt().isBefore(measurement.exchange().clientSentAt()));
            assertTrue(!measurement.exchange().serverTransmittedAt().isBefore(measurement.exchange().serverReceivedAt()));
            responder.get();
        }
    }

    @Test
    void failsClosedWhenConfiguredServerDoesNotAnswer() throws Exception {
        try (DatagramSocket unused = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            int port = unused.getLocalPort();
            unused.close();
            ObservatorioNacionalNtpClient client = new ObservatorioNacionalNtpClient(
                    List.of(InetAddress.getLoopbackAddress().getHostAddress()),
                    port,
                    Duration.ofMillis(50),
                    Clock.systemUTC()
            );

            IOException failure = assertThrows(IOException.class, client::measure);
            assertTrue(failure.getMessage().contains("Unable to obtain NTP response"));
        }
    }

    @Test
    void ntpTimestampRoundTripKeepsSubsecondPrecision() {
        Instant original = Instant.parse("2026-09-04T06:40:36.123456789Z");
        byte[] packet = new byte[48];

        ObservatorioNacionalNtpClient.writeTimestamp(packet, 0, original);
        Instant decoded = ObservatorioNacionalNtpClient.readTimestamp(packet, 0);

        assertTrue(Math.abs(Duration.between(decoded, original).toNanos()) <= 1);
    }

    @Test
    void acceptsSynchronizedServerResponse() {
        Instant t1 = Instant.parse("2026-09-04T06:40:36Z");
        byte[] response = validResponse(t1);

        assertDoesNotThrow(() -> ObservatorioNacionalNtpClient.validateResponse(response, t1));
    }

    @Test
    void rejectsInvalidPacketShapeAndProtocolMetadata() {
        Instant t1 = Instant.parse("2026-09-04T06:40:36Z");
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(null, t1));
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(new byte[47], t1));

        byte[] unsynchronized = validResponse(t1);
        unsynchronized[0] = (byte) 0xE4;
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(unsynchronized, t1));

        byte[] oldVersion = validResponse(t1);
        oldVersion[0] = 0x14; // LI=0, VN=2, mode=4
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(oldVersion, t1));

        byte[] clientMode = validResponse(t1);
        clientMode[0] = 0x23;
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(clientMode, t1));

        byte[] kissOfDeath = validResponse(t1);
        kissOfDeath[1] = 0;
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(kissOfDeath, t1));

        byte[] invalidStratum = validResponse(t1);
        invalidStratum[1] = 16;
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(invalidStratum, t1));
    }

    @Test
    void rejectsOriginateMismatchAndImpossibleServerOrdering() {
        Instant t1 = Instant.parse("2026-09-04T06:40:36Z");

        byte[] mismatch = validResponse(t1);
        ObservatorioNacionalNtpClient.writeTimestamp(mismatch, 24, t1.minusSeconds(1));
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(mismatch, t1));

        byte[] backwards = validResponse(t1);
        ObservatorioNacionalNtpClient.writeTimestamp(backwards, 32, t1.plusMillis(20));
        ObservatorioNacionalNtpClient.writeTimestamp(backwards, 40, t1.plusMillis(10));
        assertThrows(IOException.class,
                () -> ObservatorioNacionalNtpClient.validateResponse(backwards, t1));
    }

    @Test
    void validatesConfigurationAndMeasurementFailClosed() {
        assertThrows(NullPointerException.class,
                () -> new ObservatorioNacionalNtpClient(null, 123, Duration.ofSeconds(1), Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,
                () -> new ObservatorioNacionalNtpClient(List.of(), 123, Duration.ofSeconds(1), Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,
                () -> new ObservatorioNacionalNtpClient(List.of("127.0.0.1"), 0, Duration.ofSeconds(1), Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,
                () -> new ObservatorioNacionalNtpClient(List.of("127.0.0.1"), 65_536, Duration.ofSeconds(1), Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,
                () -> new ObservatorioNacionalNtpClient(List.of("127.0.0.1"), 123, Duration.ZERO, Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,
                () -> new ObservatorioNacionalNtpClient(List.of("127.0.0.1"), 123, Duration.ofSeconds(-1), Clock.systemUTC()));
        assertThrows(NullPointerException.class,
                () -> new ObservatorioNacionalNtpClient(List.of("127.0.0.1"), 123, Duration.ofSeconds(1), null));
        assertThrows(IllegalArgumentException.class,
                () -> new ObservatorioNacionalNtpClient.Measurement(" ", validExchange()));
        assertThrows(NullPointerException.class,
                () -> new ObservatorioNacionalNtpClient.Measurement("127.0.0.1", null));
    }

    private static void respondOnce(DatagramSocket server) {
        try {
            byte[] requestBytes = new byte[48];
            DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length);
            server.receive(request);

            Instant t1 = ObservatorioNacionalNtpClient.readTimestamp(requestBytes, 40);
            byte[] response = validResponse(t1);
            DatagramPacket reply = new DatagramPacket(
                    response,
                    response.length,
                    request.getAddress(),
                    request.getPort()
            );
            server.send(reply);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] validResponse(Instant t1) {
        byte[] response = new byte[48];
        response[0] = 0x24; // LI=0, VN=4, mode=4 (server)
        response[1] = 2;
        ObservatorioNacionalNtpClient.writeTimestamp(response, 24, t1);
        ObservatorioNacionalNtpClient.writeTimestamp(response, 32, t1.plusMillis(10));
        ObservatorioNacionalNtpClient.writeTimestamp(response, 40, t1.plusMillis(12));
        return response;
    }

    private static NtpExchange validExchange() {
        Instant t1 = Instant.parse("2026-09-04T06:40:36Z");
        return new NtpExchange(t1, t1.plusMillis(10), t1.plusMillis(12), t1.plusMillis(20));
    }
}
