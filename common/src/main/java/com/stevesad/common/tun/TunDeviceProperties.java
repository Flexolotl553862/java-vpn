package com.stevesad.common.tun;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tun")
public class TunDeviceProperties {

    private boolean autoStartup = false;

    private String libName = "tun_device";

    private Mock mock = Mock.NONE;

    public enum Mock {
        ECHO,
        PING,
        NONE
    }
}
