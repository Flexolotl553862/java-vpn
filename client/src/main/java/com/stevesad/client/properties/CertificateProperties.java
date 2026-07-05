package com.stevesad.client.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Getter
@Setter
@ConfigurationProperties(prefix = "vpn.client")
public class CertificateProperties {

    private Path certificatePath;

    private Path privateKeyPath;
}
