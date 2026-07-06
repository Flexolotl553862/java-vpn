package com.stevesad.client.quic;

import com.stevesad.client.properties.CertificateProperties;
import com.stevesad.client.properties.RemoteServerProperties;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.quic.QuicClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({RemoteServerProperties.class, CertificateProperties.class})
public class QuicClientConfiguration {

    private final RemoteServerProperties remoteServerProperties;
    private final CertificateProperties certificateProperties;

    @Bean
    public QuicClient quicClient() {
        QuicSslContext clientCtx = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .keyManager(
                        certificateProperties.getPrivateKeyPath().toFile(),
                        null,
                        certificateProperties.getCertificatePath().toFile())
                .applicationProtocols("h3")
                .build();

        return QuicClient.create()
                .idleTimeout(Duration.of(30, ChronoUnit.SECONDS))
                .initialSettings(spec -> spec.maxData(10000000).maxStreamDataBidirectionalLocal(1000000))
                .secure(clientCtx)
                .wiretap(true)
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
