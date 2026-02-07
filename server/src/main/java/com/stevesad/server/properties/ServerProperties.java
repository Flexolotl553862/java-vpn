package com.stevesad.server.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "vpn.server")
public class ServerProperties {

    private String host = "localhost";

    private int port = 8083;
}
