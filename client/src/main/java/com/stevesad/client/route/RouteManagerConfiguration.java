package com.stevesad.client.route;

import com.stevesad.common.tun.TunDeviceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class RouteManagerConfiguration {

    private final TunDeviceProperties tunDeviceProperties;

    @Bean
    public RouteManager routeManager() {

        if (!tunDeviceProperties.getMock().equals(TunDeviceProperties.Mock.NONE)) {
            return new MockRouteManager();
        }

        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        if (osName.contains("mac")) {
            return new MacOsRouteManager();
        }
        if (osName.contains("nix") || osName.contains("nux")) {
            return new LinuxRouteManager();
        }

        throw new IllegalStateException("Unsupported operating system: " + System.getProperty("os.name"));
    }
}
