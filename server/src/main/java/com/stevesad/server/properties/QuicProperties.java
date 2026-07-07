package com.stevesad.server.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "quic")
public class QuicProperties {

    private final Duration idleTimeout = Duration.of(30, ChronoUnit.SECONDS);

    private final long maxData = 10000000;

    private final long maxDataBidirectional = 1000000;

    private final long maxStreamsBidirectional = 1000;
}
