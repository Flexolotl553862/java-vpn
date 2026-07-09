package com.stevesad.client.quic;

import com.stevesad.common.consumer.ByteBufStreamDecoder;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicClient;
import reactor.netty.quic.QuicConnection;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuicClientRunner implements ApplicationRunner {

    private final QuicClient quicClient;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;

    private QuicConnection clientConnection;

    @Override
    public void run(ApplicationArguments args) {
        clientConnection = quicClient.connectNow();

        clientConnection
                .createStream((in, out) -> {
                    var byteBufDecoder = new ByteBufStreamDecoder(tunPacketConsumer::handle);

                    var fromTunFlux = out.send(tunPacketPublisher.subscribeAll());

                    var toTunFlux = in.receive().doOnNext(buf -> {
                        buf.retain();
                        byteBufDecoder.handle(buf);
                    });

                    return Mono.when(fromTunFlux, toTunFlux);
                })
                .subscribe();

        clientConnection.onDispose().block();
    }

    @PreDestroy
    public void shutDown() {
        clientConnection.disposeNow();
    }
}
