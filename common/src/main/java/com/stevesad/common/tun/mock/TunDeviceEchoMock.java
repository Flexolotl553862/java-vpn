package com.stevesad.common.tun.mock;

import com.stevesad.common.tun.TunDevice;
import com.stevesad.common.tun.TunDeviceProperties;
import io.netty.buffer.Unpooled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Component
@EnableConfigurationProperties(TunDeviceProperties.class)
@ConditionalOnProperty(name = "tun.mock", havingValue = "echo")
public class TunDeviceEchoMock implements TunDevice {

    private final Queue<byte[]> packetQueue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition queueEmptyCondition = lock.newCondition();

    private volatile boolean closed = false;

    private int readPacketFromQueue(ByteBuffer readBuffer, int writerIndex) {
        if (packetQueue.isEmpty()) {
            return 0;
        }

        var packet = packetQueue.poll();
        readBuffer.put(writerIndex, packet);
        return packet.length;
    }

    @Override
    public int receive(ByteBuffer readBuffer, int writerIndex) throws IOException {
        if (!packetQueue.isEmpty()) {
            return readPacketFromQueue(readBuffer, writerIndex);
        }

        lock.lock();
        try {
            while (packetQueue.isEmpty() && !closed) {
                queueEmptyCondition.await();
            }
            return readPacketFromQueue(readBuffer, writerIndex);
        } catch (InterruptedException e) {
            throw new IOException("Packet reading from Tun was interrupted", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int send(ByteBuffer packetBuffer, int readerIndex, int readableBytes) throws IOException {
        var data = new byte[readableBytes];
        packetBuffer.get(data, readerIndex, readableBytes);

        try {
            lock.lock();
            packetQueue.add(MockUtils.revertPacket(Unpooled.wrappedBuffer(data)).getRawData());
            queueEmptyCondition.signal();
            return readableBytes;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        try {
            lock.lock();
            queueEmptyCondition.signal();
            closed = true;
        } finally {
            lock.unlock();
        }
    }
}
