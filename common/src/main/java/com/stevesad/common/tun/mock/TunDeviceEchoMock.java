package com.stevesad.common.tun.mock;

import com.stevesad.common.tun.TunDevice;
import io.netty.buffer.Unpooled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@ConditionalOnProperty(name = "tun.mock", havingValue = "echo")
public class TunDeviceEchoMock implements TunDevice {

    private final BlockingQueue<byte[]> packetQueue = new LinkedBlockingQueue<>();

    private volatile boolean closed = false;

    @Override
    public int receive(ByteBuffer readBuffer, int writerIndex) throws IOException {
        if (closed) {
            throw new IOException("Tun device already closed");
        }

        try {
            var packet = packetQueue.take();
            readBuffer.put(writerIndex, packet);
            return packet.length;
        } catch (InterruptedException e) {
            throw new IOException("Packet reading from Tun was interrupted", e);
        }
    }

    @Override
    public int send(ByteBuffer packetBuffer, int readerIndex, int readableBytes) throws IOException {
        if (closed) {
            throw new IOException("Tun device already closed");
        }

        try {
            var data = new byte[readableBytes];
            packetBuffer.get(data, readerIndex, readableBytes);
            packetQueue.add(MockUtils.revertPacket(Unpooled.wrappedBuffer(data)).getRawData());
            return readableBytes;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
