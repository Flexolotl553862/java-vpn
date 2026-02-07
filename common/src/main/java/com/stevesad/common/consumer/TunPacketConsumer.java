package com.stevesad.common.consumer;

import com.stevesad.common.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunPacketConsumer {

    private final TunDevice tunDevice;

    private final ConcurrentLinkedQueue<ByteBuf> packetQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService pollingThread = Executors.newSingleThreadExecutor();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition queueEmptyCondition = lock.newCondition();

    @PostConstruct
    public void startPollingLoop() {
        pollingThread.execute(() -> {
            while (!Thread.interrupted()) {
                lock.lock();
                try {
                    while (packetQueue.isEmpty()) {
                        queueEmptyCondition.await();
                    }
                    ByteBuf packet = packetQueue.poll();
                    sendNettyBuf(packet);
                    packet.release();
                } catch (IOException e) {
                    log.error(e.getMessage());
                } catch (InterruptedException ignored) {

                } finally {
                    lock.unlock();
                }
            }
        });
    }

    private void sendNettyBuf(ByteBuf nettyBuf) throws IOException {
        ByteBuffer nioBuffer = nettyBuf.internalNioBuffer(0, nettyBuf.capacity());
        tunDevice.send(nioBuffer, nioBuffer.position() + nettyBuf.readerIndex(), nettyBuf.readableBytes());
    }

    @PreDestroy
    public void stop() {
        pollingThread.shutdown();
        pollingThread.shutdownNow();
    }

    public void handleSingle(ByteBuf buf) {
        queueNotEmptyNotify();
        packetQueue.add(buf);
    }

    public void handleBatch(ByteBuf batch) {
        queueNotEmptyNotify();
        // TODO
    }

    private void queueNotEmptyNotify() {
        try {
            lock.lock();
            queueEmptyCondition.signal();
        } finally {
            lock.unlock();
        }
    }
}
