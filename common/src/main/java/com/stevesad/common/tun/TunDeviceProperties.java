package com.stevesad.common.tun;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tun")
public class TunDeviceProperties {

    private boolean autoStartup = false;

    private Inet4Address address = Inet4Address.ofLiteral("127.0.0.1");

    private int maskLength = 32;

    private int mtu = 1400;

    private String libName = "tun_device";

    private Mock mock = Mock.NONE;

    public enum Mock {
        ECHO,
        PING,
        NONE
    }
}
