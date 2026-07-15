package com.stevesad.client.storage;

import com.stevesad.common.utils.CertificateUtils;
import lombok.*;

import java.net.Inet4Address;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VpnProfile {

    private String name;

    private String serverHost;

    private int serverPort;

    private Path certificatePath;

    private Path privateKeyPath;

    private List<String> routes = new ArrayList<>(List.of("127.0.0.1/1", "128.0.0.1/1"));

    @Override
    public String toString() {
        return name;
    }

    public Inet4Address getClientAddress() throws Exception {
        X509Certificate cert = CertificateUtils.parseCertificate(Files.readString(certificatePath));
        return Inet4Address.ofLiteral(CertificateUtils.extractCN(cert));
    }
}
