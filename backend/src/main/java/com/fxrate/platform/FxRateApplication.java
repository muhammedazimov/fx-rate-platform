package com.fxrate.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.fxrate.platform.producer.config.RateProducerProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RateProducerProperties.class)
public class FxRateApplication {

    public static void main(String[] args) {
        SpringApplication.run(FxRateApplication.class, args);
    }

}
