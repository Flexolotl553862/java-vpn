package com.stevesad.client.quic;

import com.stevesad.client.connection.VpnConnection;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.common.tun.TunDevice;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicClient;
import reactor.netty.quic.QuicConnection;
import reactor.netty.quic.QuicInbound;
import reactor.netty.quic.QuicOutbound;

import java.util.function.BiFunction;

public class QuicVpnConnection implements VpnConnection {

    private final QuicConnection connection;
    private final Disposable streamSubscription;

    public QuicVpnConnection(
            QuicClient client,
            BiFunction<QuicInbound, QuicOutbound, Mono<Void>> handler) {

        this.connection = client.connectNow();
        streamSubscription = connection.createStream(handler).subscribe();
    }

    @Override
    public void blockUntilClose() {
        connection.onDispose().block();
    }

    @Override
    public void close() {
        connection.disposeNow();
        streamSubscription.dispose();
    }
}
