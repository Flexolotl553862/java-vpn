package com.stevesad.client.quic;

import com.stevesad.client.properties.CertificateProperties;
import com.stevesad.client.properties.QuicProperties;
import com.stevesad.client.properties.RemoteServerProperties;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.quic.QuicClient;

import java.net.InetSocketAddress;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuicClientConfiguration {

    private final RemoteServerProperties remoteServerProperties;
    private final CertificateProperties certificateProperties;
    private final QuicProperties quicProperties;

    @Bean
    public QuicSslContext sslContext() {
        return QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .keyManager(
                        certificateProperties.getPrivateKeyPath().toFile(),
                        null,
                        certificateProperties.getCertificatePath().toFile())
                .applicationProtocols("h3")
                .build();
    }

    @Bean
    public QuicClient quicClient(QuicSslContext sslContext) {
        return QuicClient.create()
                .idleTimeout(quicProperties.getIdleTimeout())
                .initialSettings(spec -> spec.maxData(quicProperties.getMaxData())
                        .maxStreamDataBidirectionalLocal(quicProperties.getMaxDataBidirectional()))
                .secure(sslContext)
                .bindAddress(() -> new InetSocketAddress(0))
                .remoteAddress(() -> new InetSocketAddress(
                        remoteServerProperties.getRemoteAddress(), remoteServerProperties.getRemotePort()))
                .doOnConnected(_ -> log.info(
                        "Connected to QUIC server {}:{}",
                        remoteServerProperties.getRemoteAddress().getHostAddress(),
                        remoteServerProperties.getRemotePort()))
                .doOnDisconnected(_ -> log.info("QUIC server disconnected"));
    }
}
