package com.stevesad.common.consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

@Slf4j
public class ByteBufStreamDecoder {

    private final Deque<ByteBuf> remaining = new ArrayDeque<>();
    private final Consumer<ByteBuf> packetConsumer;

    private int totalLength;

    public ByteBufStreamDecoder(Consumer<ByteBuf> packetConsumer) {
        this.packetConsumer = packetConsumer;
    }

    public void handle(ByteBuf part) {
        remaining.addLast(part);
        totalLength += part.readableBytes();

        int length;
        while ((length = tryExtractIpV4()) > 0) {
            packetConsumer.accept(readPrefix(length));
        }
    }

    private ByteBuf readPrefix(int length) {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(length);
        while (length > 0) {
            var buf1 = remaining.getFirst();
            int read = Math.min(buf1.readableBytes(), length);

            buf.writeBytes(buf1, read);
            totalLength -= read;
            length -= read;

            if (buf1.readableBytes() == 0) {
                remaining.removeFirst().release();
            }
        }
        return buf;
    }

    private int tryExtractIpV4() {
        if (totalLength < 4) {
            return 0;
        }

        Byte f = getByte(2);
        Byte s = getByte(3);

        if (f == null || s == null) {
            return 0;
        }

        int length = ((f & 0xff) << 8 | s & 0xff);

        if (length > totalLength) {
            return 0;
        }

        return length;
    }

    private Byte getByte(int idx) {
        for (ByteBuf part : remaining) {
            if (idx < part.readableBytes()) {
                return part.getByte(part.readerIndex() + idx);
            }
            idx -= part.readableBytes();
        }
        return null;
    }
}
