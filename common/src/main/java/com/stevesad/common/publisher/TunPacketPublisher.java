package com.stevesad.common.publisher;

import com.stevesad.common.tun.TunDevice;
import com.stevesad.common.tun.TunDeviceProperties;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(PacketBatchingProperties.class)
public class TunPacketPublisher {

    private final TunDevice tunDevice;
    private final TunDeviceProperties tunDeviceProperties;
    private final PacketBatchingProperties packetBatchingProperties;

    private final ExecutorService pollingThread = Executors.newSingleThreadExecutor();
    private final Sinks.Many<ByteBuf> hotSource = Sinks.many().multicast().onBackpressureBuffer();

    @PostConstruct
    public void startPollingLoop() {
        pollingThread.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    var emitResult = hotSource.tryEmitNext(receiveNettyBuf());
                    if (emitResult.isFailure() && !emitResult.equals(Sinks.EmitResult.FAIL_OVERFLOW)) {
                        log.warn("Tun publisher emit failure: {}", emitResult.name());
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

    private ByteBuf receiveNettyBuf() throws IOException {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(2 * tunDeviceProperties.getMtu());
        buf.clear();

        int oldWriterIndex = buf.writerIndex();
        ByteBuffer internalBuffer = buf.internalNioBuffer(0, buf.capacity());
        int receivedBytes = tunDevice.receive(internalBuffer, internalBuffer.position());

        buf.writerIndex(oldWriterIndex + receivedBytes);
        return buf;
    }

    @PreDestroy
    public void stopPollingLoop() {
        pollingThread.shutdown();
        pollingThread.shutdownNow();
        hotSource.tryEmitComplete();
    }

    public Flux<ByteBuf> subscribeSingle() {
        return hotSource.asFlux();
    }

    public Flux<ByteBuf> subscribeBatch() {
        return hotSource
                .asFlux()
                .bufferTimeout(
                        packetBatchingProperties.getMaxBatchSize(), packetBatchingProperties.getMaxTimeInterval())
                .flatMap(packets -> {
                    log.debug(
                            "Sent {} packets batch with total size {}",
                            packets.size(),
                            packets.stream().collect(Collectors.summarizingInt(ByteBuf::readableBytes)));

                    return Mono.just(PooledByteBufAllocator.DEFAULT
                            .compositeHeapBuffer(packets.size())
                            .addComponents(true, packets));
                });
    }
}
