package com.stevesad.server.ssl;

import com.stevesad.common.utils.CertificateUtils;
import com.stevesad.server.domain.CaCertificate;
import com.stevesad.server.domain.ClientCertificate;
import com.stevesad.server.repository.CaCertificateRepository;
import com.stevesad.server.repository.ClientCertificateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicTrustManager implements X509TrustManager {

    private final CaCertificateRepository caCertificateRepository;
    private final ClientCertificateRepository clientCertificateRepository;

    private X509TrustManager caTrustManager;
    private X509Certificate[] acceptedIssuers = new X509Certificate[0];
    private final Set<String> trustedClientFingerprints = new HashSet<>();

    @PostConstruct
    public void init() throws Exception {
        List<CaCertificate> caCertificates = caCertificateRepository.getAllCaCertificates();
        if (caCertificates.isEmpty()) {
            throw new IllegalStateException("No CA certificates configured");
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        for (CaCertificate caCertificate : caCertificates) {
            X509Certificate certificate = CertificateUtils.parseCertificate(caCertificate.getCertPem());

            try {
                certificate.checkValidity();
                trustStore.setCertificateEntry(caCertificate.getId().toString(), certificate);
            } catch (Exception ignored) {
            }
        }

        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);

        caTrustManager = Arrays.stream(factory.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No X509TrustManager available"));

        acceptedIssuers = caTrustManager.getAcceptedIssuers().clone();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Client certificate chain is empty");
        }

        caTrustManager.checkClientTrusted(chain, authType);

        if (!trustedClientFingerprints.contains(fingerprint(chain[0]))) {
            throw new CertificateException("Client certificate is not trusted");
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        caTrustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return acceptedIssuers.clone();
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    private void loadTrustedClientCerts() {
        List<ClientCertificate> certs = clientCertificateRepository.getTrusted();

        trustedClientFingerprints.addAll(certs.stream()
                .flatMap(c -> {
                    try {
                        X509Certificate cert = CertificateUtils.parseCertificate(c.getCertPem());
                        String fingerprint = fingerprint(cert);
                        return Stream.of(fingerprint);
                    } catch (CertificateException e) {
                        log.warn("Failed to calculate fingerprint, certId={}", c.getId());
                    }
                    return Stream.empty();
                })
                .toList());

        log.debug("Client certificates refreshed");
    }

    private static String fingerprint(X509Certificate certificate) throws CertificateException {
        try {
            return java.util.HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
        } catch (java.security.GeneralSecurityException e) {
            throw new CertificateException("Failed to calculate certificate fingerprint", e);
        }
    }
}
