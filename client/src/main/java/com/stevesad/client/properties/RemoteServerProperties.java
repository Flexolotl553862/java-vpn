package com.stevesad.client.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vpn.server")
public class RemoteServerProperties {

    private InetAddress remoteAddress;

    private int remotePort;
}
