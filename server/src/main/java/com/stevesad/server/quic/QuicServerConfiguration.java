package com.stevesad.server.quic;

import com.stevesad.common.consumer.ByteBufStreamDecoder;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.server.properties.ServerProperties;
import io.netty.handler.codec.quic.InsecureQuicTokenHandler;
import io.netty.handler.codec.quic.QuicSslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ServerProperties.class)
public class QuicServerConfiguration {

    private final ServerProperties properties;
    private final QuicSslContext sslContext;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;

    @Bean
    public QuicServer quicServer() {
        return QuicServer.create()
                .host(properties.getHost())
                .port(properties.getPort())
                .secure(sslContext)
                .idleTimeout(Duration.of(30, ChronoUnit.SECONDS))
                .initialSettings(spec -> spec.maxData(10000000)
                        .maxStreamDataBidirectionalRemote(1000000)
                        .maxStreamsBidirectional(100))
                .wiretap(true)
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                .doOnBind(_ -> log.info("Started QUIC server on {}:{}", properties.getHost(), properties.getPort()))
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
