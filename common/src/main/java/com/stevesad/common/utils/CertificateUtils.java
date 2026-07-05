package com.stevesad.common.utils;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateUtils {

    private static final CertificateFactory certificateFactory;

    static {
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException("Failed to create certificate factory", e);
        }
    }

    public static X509Certificate parseCertificate(String certPem) throws CertificateException {
        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certPem.getBytes()));
    }

    public static PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        try (PEMParser parser = new PEMParser(new StringReader(privateKeyPem))) {
            PEMKeyPair keyPair = (PEMKeyPair) parser.readObject();
            return new JcaPEMKeyConverter().getPrivateKey(keyPair.getPrivateKeyInfo());
        }
    }
}
