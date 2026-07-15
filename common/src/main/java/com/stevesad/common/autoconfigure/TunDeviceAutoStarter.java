package com.stevesad.common.autoconfigure;

import com.stevesad.common.launcher.TunLauncher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tun.auto.enabled", havingValue = "true")
public class TunDeviceAutoStarter {

    private final TunLauncher launcher;
    private final TunDeviceAutoStartProperties autoStartProperties;

    @PostConstruct
    public void autoStartup() throws Exception {
        launcher.start(
                autoStartProperties.getAddress().getHostAddress(),
                autoStartProperties.getMaskLength(),
                autoStartProperties.getMtu());
    }

    @PreDestroy
    public void autoShutdown() {
        launcher.stop();
    }
}
