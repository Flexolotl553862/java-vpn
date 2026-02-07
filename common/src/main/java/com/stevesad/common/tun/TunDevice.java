package com.stevesad.common.tun;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface TunDevice extends AutoCloseable {

    int receive(ByteBuffer readBuffer, int writerIndex) throws IOException;

    int send(ByteBuffer packetBuffer, int readerIndex, int readableBytes) throws IOException;
}
