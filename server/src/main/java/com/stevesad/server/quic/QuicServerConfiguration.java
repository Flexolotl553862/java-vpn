package com.stevesad.server.quic;

import com.stevesad.common.consumer.ByteBufStreamDecoder;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.server.properties.QuicProperties;
import com.stevesad.server.properties.ServerProperties;
import io.netty.handler.codec.quic.InsecureQuicTokenHandler;
import io.netty.handler.codec.quic.QuicSslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuicServerConfiguration {

    private final ServerProperties serverProperties;
    private final QuicProperties quicProperties;
    private final QuicSslContext sslContext;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;

    @Bean
    public QuicServer quicServer() {
        return QuicServer.create()
                .host(serverProperties.getHost())
                .port(serverProperties.getPort())
                .secure(sslContext)
                .idleTimeout(quicProperties.getIdleTimeout())
                .initialSettings(spec -> spec.maxData(quicProperties.getMaxData())
                        .maxStreamDataBidirectionalRemote(quicProperties.getMaxDataBidirectional())
                        .maxStreamsBidirectional(quicProperties.getMaxStreamsBidirectional()))
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                .doOnBind(_ -> log.info(
                        "Started QUIC server on {}:{}", serverProperties.getHost(), serverProperties.getPort()))
                .handleStream((in, out) -> {
                    var byteBufDecoder = new ByteBufStreamDecoder(tunPacketConsumer::handle);

                    var toTunFlux = in.receive().doOnNext(p -> {
                        p.retain();
                        byteBufDecoder.handle(p);
                    });

                    var fromTunFlux = out.send(tunPacketPublisher.subscribe());

                    return Mono.when(fromTunFlux, toTunFlux);
                });
    }
}
