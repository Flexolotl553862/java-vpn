package com.stevesad.common.publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NettyBufAllocator {

    private static final int INTERNAL_BUF_SIZE = 4 * 1024 * 1024;

    private ByteBuf internalBuf;

    public ByteBuf allocate(int maxSize) {
        if (internalBuf == null || internalBuf.writableBytes() < maxSize) {
            clear();
            internalBuf = ByteBufAllocator.DEFAULT.directBuffer(INTERNAL_BUF_SIZE);
        }

        return internalBuf;
    }

    public void clear() {
        if (internalBuf != null) {
            internalBuf.release();
        }
    }
}
