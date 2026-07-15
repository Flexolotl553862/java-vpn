package com.stevesad.common.publisher;

import com.stevesad.common.tun.TunDevice;
import com.stevesad.common.utils.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunPacketPublisher {

    @Setter
    private int packetBufferSize = 2000;

    private final TunDevice tunDevice;

    private ExecutorService pollingThread;
    private final Map<InetAddress, Sinks.Many<ByteBuf>> sinkByAddress = new ConcurrentHashMap<>();
    private final Sinks.Many<ByteBuf> hotSource =
            Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

    public synchronized void startPollingLoop() {
        if (pollingThread != null && !pollingThread.isShutdown()) {
            return;
        }

        pollingThread = Executors.newSingleThreadExecutor();
        pollingThread.execute(() -> {
            while (!Thread.interrupted()) {
                ByteBuf packet = null;

                try {
                    packet = receiveNettyBuf();
                    var address = PacketUtils.extractIpV4DstAddress(packet);
                    var sink = sinkByAddress.get(address);

                    if (sink != null && sink.currentSubscriberCount() == 0) {
                        sinkByAddress.remove(address, sink);
                        sink = null;
                    }

                    if (sink == null) {
                        sink = hotSource;
                    }

                    var emitResult = sink.tryEmitNext(packet);
                    if (emitResult.isFailure()) {
                        packet.release();
                    }

                    if (emitResult.isFailure() && !emitResult.equals(Sinks.EmitResult.FAIL_OVERFLOW)) {
                        log.warn("Tun publisher emit failure: {}", emitResult.name());
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    if (packet != null) {
                        packet.release();
                    }
                }
            }
        });
    }

    private ByteBuf receiveNettyBuf() throws IOException {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(packetBufferSize);
        buf.clear();

        int oldWriterIndex = buf.writerIndex();
        ByteBuffer internalBuffer = buf.internalNioBuffer(0, buf.capacity());
        int receivedBytes = tunDevice.receive(internalBuffer, internalBuffer.position());

        buf.writerIndex(oldWriterIndex + receivedBytes);
        return buf;
    }

    public synchronized void stopPollingLoop() {
        if (pollingThread == null) {
            return;
        }

        ExecutorService executor = pollingThread;
        executor.shutdownNow();

        while (true) {
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("Tun publisher polling thread did not stop in time");
                }
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        pollingThread = null;
        sinkByAddress.forEach((_, sink) -> sink.tryEmitComplete());
        sinkByAddress.clear();
    }

    public Flux<ByteBuf> subscribeAll() {
        return hotSource.asFlux();
    }

    public Flux<ByteBuf> subscribeByAddress(InetAddress inetAddress) {
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();
        sinkByAddress.put(inetAddress, sink);
        return sink.asFlux();
    }
}
