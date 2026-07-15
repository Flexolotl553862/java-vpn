package com.stevesad.common.tun.mock;

import com.stevesad.common.tun.TunDevice;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.packet.IllegalRawDataException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ConditionalOnProperty(name = "tun.mock", havingValue = "ping")
@RequiredArgsConstructor
public class TunDevicePingMock implements TunDevice {

    private String address;

    private final Map<Short, Instant> unhandledRequests = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(1);
    private volatile boolean closed = true;

    @Setter
    private Duration PING_DURATION = Duration.ofMillis(1000);

    @Override
    public void start(String address, int maskLength, int mtu) {
        closed = false;
        this.address = address;
        log.info("Opened Tun device with mock mode: PING");
    }

    @Override
    public int receive(ByteBuffer readBuffer, int writerIndex) throws IOException {
        if (closed) {
            throw new IOException("Tun device already closed");
        }

        try {
            Thread.sleep(PING_DURATION);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Waiting was interrupted");
            throw new IOException("Packet reading from Tun was interrupted", e);
        }

        if (closed) {
            throw new IOException("Tun device already closed");
        }

        short seq = (short) counter.getAndIncrement();

        var packet = MockUtils.createIcmpEchoPacket(address, "8.8.8.8", seq, (short) 0)
                .getRawData();

        unhandledRequests.put(seq, Instant.now());

        readBuffer.put(writerIndex, packet);
        return packet.length;
    }

    @Override
    public int send(ByteBuffer packetBuffer, int readerIndex, int readableBytes) throws IOException {
        byte[] data = new byte[readableBytes];
        packetBuffer.get(data);

        try {
            var reply = MockUtils.readRawIcmpEchoPacket(data);

            var requestTime = unhandledRequests.get(reply.getHeader().getSequenceNumber());

            if (requestTime == null) {
                log.warn("Request time for seq {} not found", reply.getHeader().getSequenceNumber());
            } else {
                log.info(
                        "echo reply seq={} time={}ms",
                        Short.toUnsignedInt(reply.getHeader().getSequenceNumber()),
                        Duration.between(requestTime, Instant.now()).toMillis());
                unhandledRequests.remove(reply.getHeader().getSequenceNumber());
            }

            return data.length;
        } catch (IllegalRawDataException e) {
            log.error("unknown protocol, not ICMP");
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
