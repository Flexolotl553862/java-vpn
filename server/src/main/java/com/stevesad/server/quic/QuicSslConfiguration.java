package com.stevesad.server.quic;

import com.stevesad.common.utils.CertificateUtils;
import com.stevesad.server.domain.ServerCertificate;
import com.stevesad.server.repository.ServerCertificateRepository;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.ssl.ClientAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.TrustManager;

@Configuration
@RequiredArgsConstructor
public class QuicSslConfiguration {

    private final ServerCertificateRepository serverCertificateRepository;
    private final TrustManager caTrustManager;

    @Bean
    public QuicSslContext quicSslContext() throws Exception {
        ServerCertificate serverCert = serverCertificateRepository
                .getAny()
                .orElseThrow(() -> new RuntimeException("No available server certificate found"));

        return QuicSslContextBuilder.forServer(
                        CertificateUtils.parsePrivateKey(serverCert.getPrivateKeyPem()),
                        null,
                        CertificateUtils.parseCertificate(serverCert.getCertPem()))
                .trustManager(caTrustManager)
                .clientAuth(ClientAuth.REQUIRE)
                .applicationProtocols("h3")
                .build();
    }
}
