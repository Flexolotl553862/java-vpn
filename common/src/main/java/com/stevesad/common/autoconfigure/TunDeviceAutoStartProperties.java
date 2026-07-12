package com.stevesad.common.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Getter
@Setter
@Component
@ConditionalOnProperty(name = "tun.auto.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "tun.auto")
public class TunDeviceAutoStartProperties {

    private boolean enabled = false;

    private InetAddress address;

    private int maskLength = 32;

    private int mtu = 1400;
}
