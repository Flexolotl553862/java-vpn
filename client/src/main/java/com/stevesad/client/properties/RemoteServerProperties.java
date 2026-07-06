package com.stevesad.client.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetAddress;

@Getter
@Setter
@ConfigurationProperties(prefix = "vpn.server")
public class RemoteServerProperties {

    private InetAddress remoteAddress;

    private int remotePort;
}
