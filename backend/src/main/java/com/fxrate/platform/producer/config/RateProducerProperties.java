package com.fxrate.platform.producer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for the simulated rate producer.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.producer")
public class RateProducerProperties {
    private boolean enabled = false;
    private long intervalMs = 1000;
    private List<String> pairs = List.of("EUR/USD", "USD/TRY", "GBP/USD");
}
