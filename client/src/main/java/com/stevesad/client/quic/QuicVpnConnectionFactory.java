package com.stevesad.client.quic;

import com.stevesad.client.connection.VpnConnection;
import com.stevesad.client.connection.VpnConnectionFactory;
import com.stevesad.client.properties.QuicProperties;
import com.stevesad.client.storage.VpnProfile;
import com.stevesad.common.consumer.ByteBufStreamDecoder;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.common.tun.TunDeviceProperties;
import com.stevesad.common.utils.CertificateUtils;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicClient;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.cert.X509Certificate;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuicVpnConnectionFactory implements VpnConnectionFactory {

    private final QuicProperties quicProperties;
    private final TunDeviceProperties tunDeviceProperties;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;

    private QuicSslContext createSslContext(VpnProfile profile) {
        return QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .keyManager(
                        profile.getPrivateKeyPath().toFile(),
                        null,
                        profile.getCertificatePath().toFile())
                .applicationProtocols("h3")
                .build();
    }

    private QuicClient createClient(VpnProfile profile) {
        return QuicClient.create()
                .idleTimeout(quicProperties.getIdleTimeout())
                .initialSettings(spec -> spec.maxData(quicProperties.getMaxData())
                        .maxStreamDataBidirectionalLocal(quicProperties.getMaxDataBidirectional()))
                .secure(createSslContext(profile))
                .bindAddress(() -> new InetSocketAddress(0))
                .remoteAddress(() -> new InetSocketAddress(profile.getServerHost(), profile.getServerPort()))
                .doOnConnected(_ ->
                        log.info("Connected to QUIC server {}:{}", profile.getServerHost(), profile.getServerPort()))
                .doOnDisconnected(_ -> log.info("QUIC server disconnected"));
    }

    @Override
    public VpnConnection openConnection(VpnProfile profile) throws Exception {
        QuicClient client = createClient(profile);
        X509Certificate cert = CertificateUtils.parseCertificate(Files.readString(profile.getCertificatePath()));
        tunDeviceProperties.setAddress(Inet4Address.ofLiteral(CertificateUtils.extractCN(cert)));

        return new QuicVpnConnection(client, (in, out) -> {
            var byteBufDecoder = new ByteBufStreamDecoder(tunPacketConsumer::handle);

            var fromTunFlux = out.send(tunPacketPublisher.subscribeAll());

            var toTunFlux = in.receive().doOnNext(buf -> {
                buf.retain();
                byteBufDecoder.handle(buf);
            });

            return Mono.when(fromTunFlux, toTunFlux);
        });
    }
}
