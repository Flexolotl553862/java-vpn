package com.stevesad.common.tun;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface TunDevice extends AutoCloseable {

    void start(String address, int maskLength, int mtu) throws IOException;

    int receive(ByteBuffer readBuffer, int writerIndex) throws IOException;

    int send(ByteBuffer packetBuffer, int readerIndex, int readableBytes) throws IOException;
}
