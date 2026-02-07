package com.stevesad.common.publisher;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@ConfigurationProperties(prefix = "batching")
public class PacketBatchingProperties {

    private Duration maxTimeInterval = Duration.of(10, ChronoUnit.MILLIS);

    private int maxBatchSize = 50;
}
