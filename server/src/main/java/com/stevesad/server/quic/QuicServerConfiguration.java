package com.stevesad.server.quic;

import com.stevesad.common.consumer.ByteBufStreamDecoder;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.common.utils.CertificateUtils;
import com.stevesad.common.utils.PacketUtils;
import com.stevesad.server.properties.QuicProperties;
import com.stevesad.server.properties.ServerProperties;
import io.netty.channel.Channel;
import io.netty.handler.codec.quic.InsecureQuicTokenHandler;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicSslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;
import reactor.netty.quic.QuicServer;

import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

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
                    Sinks.One<InetAddress> clientAddressSink = Sinks.one();
                    AtomicReference<InetAddress> clientAddressRef = new AtomicReference<>();

                    var byteBufDecoder = new ByteBufStreamDecoder(p -> {
                        try {
                            var srcAddress = PacketUtils.extractIpV4SrcAddress(p);
                            if (srcAddress.equals(clientAddressRef.get())) {
                                tunPacketConsumer.handle(p);
                            } else {
                                p.release();
                            }
                        } catch (Exception e) {
                            p.release();
                            log.warn("Failed to extract src address");
                        }
                    });

                    in.withConnection(conn -> {
                        X509Certificate clientCert = extractClientCertificate(conn);

                        try {
                            clientAddressRef.set(InetAddress.ofLiteral(CertificateUtils.extractCN(clientCert)));
                            clientAddressSink.tryEmitValue(clientAddressRef.get());
                        } catch (Exception e) {
                            log.warn("Could not extract client address from auth certificate");
                            clientAddressSink.tryEmitError(e);
                        }
                    });

                    var toTunFlux = in.receive().doOnNext(p -> {
                        p.retain();
                        byteBufDecoder.handle(p);
                    });

                    var fromTunFlux =
                            out.send(clientAddressSink.asMono().flatMapMany(tunPacketPublisher::subscribeByAddress));

                    return Mono.when(fromTunFlux, toTunFlux);
                });
    }

    private X509Certificate extractClientCertificate(Connection conn) {
        Channel ch = conn.channel();
        X509Certificate cert = null;

        while (ch != null && cert == null) {
            if (ch instanceof QuicChannel quicCh) {
                try {
                    Certificate[] certs = Objects.requireNonNull(quicCh.sslEngine())
                            .getSession()
                            .getPeerCertificates();
                    for (var crt : certs) {
                        if (crt instanceof X509Certificate x509) {
                            cert = x509;
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            ch = ch.parent();
        }

        return cert;
    }
}
