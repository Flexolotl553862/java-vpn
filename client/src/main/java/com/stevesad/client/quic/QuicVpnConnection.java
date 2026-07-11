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
    private final TunDevice tunDevice;
    private final TunPacketPublisher publisher;
    private final TunPacketConsumer consumer;
    private final Disposable streamSubscription;

    public QuicVpnConnection(
            QuicClient client,
            BiFunction<QuicInbound, QuicOutbound, Mono<Void>> handler,
            TunDevice tunDevice,
            TunPacketPublisher publisher,
            TunPacketConsumer consumer) throws Exception {

        this.connection = client.connectNow();
        this.tunDevice = tunDevice;
        this.publisher = publisher;
        this.consumer = consumer;

        try {
            streamSubscription = connection.createStream(handler).subscribe();
            tunDevice.start();
            publisher.startPollingLoop();
            consumer.startPollingLoop();
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    @Override
    public void blockUntilClose() {
        connection.onDispose().block();
    }

    @Override
    public void close() throws Exception {
        Exception failure = null;

        try {
            if (streamSubscription != null) {
                streamSubscription.dispose();
            }
        } catch (Exception e) {
            failure = e;
        }

        try {
            publisher.stopPollingLoop();
        } catch (Exception e) {
            failure = addSuppressed(failure, e);
        }

        try {
            consumer.stopPollingLoop();
        } catch (Exception e) {
            failure = addSuppressed(failure, e);
        }

        try {
            tunDevice.close();
        } catch (Exception e) {
            failure = addSuppressed(failure, e);
        }

        try {
            connection.disposeNow();
        } catch (Exception e) {
            failure = addSuppressed(failure, e);
        }

        if (failure != null) {
            throw failure;
        }
    }

    private Exception addSuppressed(Exception failure, Exception next) {
        if (failure == null) {
            return next;
        }

        failure.addSuppressed(next);
        return failure;
    }
}
