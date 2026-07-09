import com.stevesad.common.utils.CertificateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

public class CertificateUtilsTest {

    private static X509Certificate cert;
    private static PrivateKey key;

    @BeforeAll
    public static void setup() throws Exception {
        try (var certStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("cert.pem");
                var keyStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("key.pem")) {
            assert certStream != null;
            assert keyStream != null;
            cert = CertificateUtils.parseCertificate(new String(certStream.readAllBytes(), StandardCharsets.UTF_8));
            key = CertificateUtils.parsePrivateKey(new String(keyStream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testExtractCN() {
        Assertions.assertEquals("10.2.1.1", CertificateUtils.extractCN(cert));
    }

    @Test
    public void testChallenge() throws Exception {
        byte[] challenge = "test".getBytes();

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(challenge);
        byte[] signature = sig.sign();

        sig.initVerify(cert.getPublicKey());
        sig.update(challenge);

        Assertions.assertTrue(sig.verify(signature));
    }
}
