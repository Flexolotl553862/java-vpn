package com.stevesad.server.api;

import com.stevesad.common.utils.CertificateUtils;
import com.stevesad.server.domain.ClientCertificate;
import com.stevesad.server.domain.ServerCertificate;
import com.stevesad.server.repository.ClientCertificateRepository;
import com.stevesad.server.repository.ServerCertificateRepository;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NettyServerConfiguration {

    private final ServerCertificateRepository serverCertificateRepository;
    private final ClientCertificateRepository clientCertificateRepository;

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() throws Exception {
        ServerCertificate serverCert = serverCertificateRepository
                .getAny()
                .orElseThrow(() -> new RuntimeException("No available server certificate found"));

        List<X509Certificate> trustedCerts = new ArrayList<>();

        for (ClientCertificate clientCert : clientCertificateRepository.getTrusted()) {
            var x509 = CertificateUtils.parseCertificate(clientCert.getCertPem());
            trustedCerts.add(x509);
        }

        SslContext sslContext = SslContextBuilder.forServer(
                        CertificateUtils.parsePrivateKey(serverCert.getPrivateKeyPem()),
                        CertificateUtils.parseCertificate(serverCert.getCertPem()))
                .trustManager(trustedCerts)
                .clientAuth(ClientAuth.REQUIRE)
                .build();

        return factory -> factory.addServerCustomizers(
                server -> server.secure(sslContextSpec -> sslContextSpec.sslContext(sslContext)));
    }
}
