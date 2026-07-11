package com.stevesad.common.consumer;

import com.stevesad.common.tun.TunDevice;
import com.stevesad.common.tun.TunDeviceProperties;
import io.netty.buffer.ByteBuf;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunPacketConsumer {

    private final TunDevice tunDevice;
    private final TunDeviceProperties tunDeviceProperties;

    private final BlockingQueue<ByteBuf> packetQueue = new LinkedBlockingQueue<>();
    private ExecutorService pollingThread;

    @PostConstruct
    public void autoStartup() {
        if (tunDeviceProperties.isAutoStartup()) {
            startPollingLoop();
        }
    }

    @PreDestroy
    public void autoShutdown() {
        if (tunDeviceProperties.isAutoStartup()) {
            stopPollingLoop();
        }
    }

    public synchronized void startPollingLoop() {
        if (pollingThread != null && !pollingThread.isShutdown()) {
            return;
        }

        pollingThread = Executors.newSingleThreadExecutor();
        pollingThread.execute(() -> {
            while (!Thread.interrupted()) {
                ByteBuf packet = null;
                try {
                    packet = packetQueue.take();
                    sendNettyBuf(packet);
                } catch (IOException e) {
                    log.error(e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (packet != null) {
                        packet.release();
                    }
                }
            }
        });
    }

    private void sendNettyBuf(ByteBuf nettyBuf) throws IOException {
        ByteBuffer nioBuffer = nettyBuf.internalNioBuffer(0, nettyBuf.capacity());
        tunDevice.send(nioBuffer, nioBuffer.position() + nettyBuf.readerIndex(), nettyBuf.readableBytes());
    }

    public synchronized void stopPollingLoop() {
        if (pollingThread == null) {
            return;
        }

        ExecutorService executor = pollingThread;
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                log.warn("Tun consumer polling thread did not stop in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pollingThread = null;
        releaseQueuedPackets();
    }

    public void handle(ByteBuf packet) {
        packetQueue.add(packet);
    }

    private void releaseQueuedPackets() {
        ByteBuf packet;
        while ((packet = packetQueue.poll()) != null) {
            packet.release();
        }
    }
}
