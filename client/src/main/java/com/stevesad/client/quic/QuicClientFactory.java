package com.stevesad.client.quic;

import com.stevesad.client.properties.QuicProperties;
import com.stevesad.client.storage.LocalStorage;
import com.stevesad.client.storage.VpnProfile;
import com.stevesad.common.consumer.ByteBufStreamDecoder;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.common.utils.CertificateUtils;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicClient;
import reactor.netty.quic.QuicInbound;
import reactor.netty.quic.QuicOutbound;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuicClientFactory {

    private final QuicProperties quicProperties;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;
    private final LocalStorage localStorage;

    @SneakyThrows
    private QuicSslContext createSslContext(VpnProfile profile) {
        List<X509Certificate> trustedCerts = new ArrayList<>();

        for (Path path : localStorage.loadAllTrustedCerts()) {
            trustedCerts.add(CertificateUtils.parseCertificate(Files.readString(path)));
        }

        return QuicSslContextBuilder.forClient()
                .trustManager(trustedCerts.toArray(new X509Certificate[0]))
                .keyManager(
                        profile.getPrivateKeyPath().toFile(),
                        null,
                        profile.getCertificateChainPath().toFile())
                .applicationProtocols("h3")
                .build();
    }

    public QuicClient createClient(VpnProfile profile) {
        return QuicClient.create()
                .idleTimeout(quicProperties.getIdleTimeout())
                .initialSettings(spec -> spec.maxData(quicProperties.getMaxData())
                        .maxStreamDataBidirectionalLocal(quicProperties.getMaxDataBidirectional()))
                .secure(channel -> createSslContext(profile).newEngine(
                        channel.alloc(),
                        profile.getServerHost(),
                        profile.getServerPort()))
                .bindAddress(() -> new InetSocketAddress(0))
                .remoteAddress(() -> new InetSocketAddress(profile.getServerHost(), profile.getServerPort()))
                .doOnConnected(_ ->
                        log.info("Connected to QUIC server {}:{}", profile.getServerHost(), profile.getServerPort()))
                .doOnDisconnected(_ -> log.info("QUIC server disconnected"));
    }

    public BiFunction<QuicInbound, QuicOutbound, Mono<Void>> createHandler() {
        return (in, out) -> {
            var byteBufDecoder = new ByteBufStreamDecoder(tunPacketConsumer::handle);

            var fromTunFlux = out.send(tunPacketPublisher.subscribeAll());

            var toTunFlux = in.receive().doOnNext(buf -> {
                buf.retain();
                byteBufDecoder.handle(buf);
            });

            return Mono.when(fromTunFlux, toTunFlux);
        };
    }
}
