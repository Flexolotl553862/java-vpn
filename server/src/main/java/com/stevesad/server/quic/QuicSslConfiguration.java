package com.stevesad.server.quic;

import com.stevesad.common.utils.CertificateUtils;
import com.stevesad.server.domain.ClientCertificate;
import com.stevesad.server.domain.ServerCertificate;
import com.stevesad.server.repository.ClientCertificateRepository;
import com.stevesad.server.repository.ServerCertificateRepository;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.ssl.ClientAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuicSslConfiguration {

    private final ServerCertificateRepository serverCertificateRepository;
    private final ClientCertificateRepository clientCertificateRepository;

    @Bean
    public QuicSslContext quicSslContext() throws Exception {
        ServerCertificate serverCert = serverCertificateRepository
                .getAny()
                .orElseThrow(() -> new RuntimeException("No available server certificate found"));

        List<X509Certificate> trustedCerts = new ArrayList<>();

        for (ClientCertificate clientCert : clientCertificateRepository.getTrusted()) {
            var x509 = CertificateUtils.parseCertificate(clientCert.getCertPem());
            trustedCerts.add(x509);
        }

        return QuicSslContextBuilder.forServer(
                        CertificateUtils.parsePrivateKey(serverCert.getPrivateKeyPem()),
                        null,
                        CertificateUtils.parseCertificate(serverCert.getCertPem()))
                .trustManager(trustedCerts.toArray(new X509Certificate[0]))
                .clientAuth(ClientAuth.REQUIRE)
                .applicationProtocols("h3")
                .build();
    }
}
