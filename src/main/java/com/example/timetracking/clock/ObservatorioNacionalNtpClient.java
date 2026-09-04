package com.example.timetracking.clock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Minimal UDP NTP transport used only to obtain trusted clock evidence.
 * Production defaults are the public Observatorio Nacional NTP endpoints.
 *
 * Network failures fail closed: no evidence is fabricated.
 */
public final class ObservatorioNacionalNtpClient {
    static final int NTP_PACKET_SIZE = 48;
    static final int NTP_PORT = 123;
    static final long NTP_EPOCH_OFFSET_SECONDS = 2_208_988_800L;

    private static final List<String> OFFICIAL_SERVERS = List.of(
            "200.20.186.75",
            "200.20.186.94"
    );

    private final List<String> servers;
    private final int port;
    private final Duration timeout;
    private final Clock clock;

    public ObservatorioNacionalNtpClient(Duration timeout) {
        this(OFFICIAL_SERVERS, NTP_PORT, timeout, Clock.systemUTC());
    }

    ObservatorioNacionalNtpClient(List<String> servers, int port, Duration timeout, Clock clock) {
        this.servers = List.copyOf(Objects.requireNonNull(servers, "servers"));
        if (servers.isEmpty()) {
            throw new IllegalArgumentException("servers must not be empty");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        this.port = port;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        long timeoutMillis = timeout.toMillis();
        if (timeout.isNegative() || timeoutMillis < 1 || timeoutMillis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("timeout must be at least 1ms and fit DatagramSocket timeout");
        }
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Measurement measure() throws IOException {
        IOException lastFailure = null;
        for (String server : servers) {
            try {
                return new Measurement(server, exchange(server));
            } catch (IOException failure) {
                lastFailure = failure;
            }
        }
        throw new IOException("Unable to obtain NTP response from configured trusted servers", lastFailure);
    }

    private NtpExchange exchange(String server) throws IOException {
        InetAddress address = InetAddress.getByName(server);
        byte[] request = new byte[NTP_PACKET_SIZE];
        request[0] = 0x23; // LI=0, VN=4, mode=3 (client)

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(Math.toIntExact(timeout.toMillis()));
            Instant t1 = clock.instant();
            writeTimestamp(request, 40, t1);
            socket.send(new DatagramPacket(request, request.length, address, port));

            byte[] response = new byte[NTP_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(response, response.length);
            try {
                socket.receive(packet);
            } catch (SocketTimeoutException timeoutFailure) {
                throw new IOException("NTP request timed out for " + server, timeoutFailure);
            }
            Instant t4 = clock.instant();

            if (!packet.getAddress().equals(address) || packet.getPort() != port) {
                throw new IOException("NTP response source does not match requested server");
            }
            validateResponse(response, t1);

            return new NtpExchange(
                    t1,
                    readTimestamp(response, 32),
                    readTimestamp(response, 40),
                    t4
            );
        }
    }

    static void validateResponse(byte[] response, Instant t1) throws IOException {
        if (response == null || response.length < NTP_PACKET_SIZE) {
            throw new IOException("Invalid NTP packet length");
        }
        int leapIndicator = (response[0] >>> 6) & 0x3;
        int version = (response[0] >>> 3) & 0x7;
        int mode = response[0] & 0x7;
        int stratum = Byte.toUnsignedInt(response[1]);

        if (leapIndicator == 3) {
            throw new IOException("NTP server reports unsynchronized clock");
        }
        if (version < 3 || version > 4) {
            throw new IOException("Unsupported NTP version");
        }
        if (mode != 4) {
            throw new IOException("NTP response is not server mode");
        }
        if (stratum == 0 || stratum > 15) {
            throw new IOException("Invalid NTP stratum");
        }

        if (!matchesEncodedTimestamp(response, 24, t1)) {
            throw new IOException("NTP originate timestamp does not match request");
        }
        Instant receive = readTimestamp(response, 32);
        Instant transmit = readTimestamp(response, 40);
        if (transmit.isBefore(receive)) {
            throw new IOException("NTP transmit timestamp precedes receive timestamp");
        }
    }

    static void writeTimestamp(byte[] packet, int offset, Instant instant) {
        long seconds = instant.getEpochSecond() + NTP_EPOCH_OFFSET_SECONDS;
        long fraction = (instant.getNano() * 0x1_0000_0000L) / 1_000_000_000L;
        writeUnsignedInt(packet, offset, seconds);
        writeUnsignedInt(packet, offset + 4, fraction);
    }

    static Instant readTimestamp(byte[] packet, int offset) {
        long seconds = readUnsignedInt(packet, offset);
        long fraction = readUnsignedInt(packet, offset + 4);
        long nanos = (fraction * 1_000_000_000L) >>> 32;
        return Instant.ofEpochSecond(seconds - NTP_EPOCH_OFFSET_SECONDS, nanos);
    }

    private static boolean matchesEncodedTimestamp(byte[] packet, int offset, Instant expected) {
        byte[] encoded = new byte[8];
        writeTimestamp(encoded, 0, expected);
        return Arrays.equals(encoded, 0, encoded.length, packet, offset, offset + encoded.length);
    }

    private static long readUnsignedInt(byte[] data, int offset) {
        return ((long) Byte.toUnsignedInt(data[offset]) << 24)
                | ((long) Byte.toUnsignedInt(data[offset + 1]) << 16)
                | ((long) Byte.toUnsignedInt(data[offset + 2]) << 8)
                | Byte.toUnsignedInt(data[offset + 3]);
    }

    private static void writeUnsignedInt(byte[] data, int offset, long value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }

    public record Measurement(String serverAddress, NtpExchange exchange) {
        public Measurement {
            if (serverAddress == null || serverAddress.isBlank()) {
                throw new IllegalArgumentException("serverAddress must not be blank");
            }
            Objects.requireNonNull(exchange, "exchange");
        }
    }
}
