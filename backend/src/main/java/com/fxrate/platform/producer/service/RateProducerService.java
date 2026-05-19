package com.fxrate.platform.producer.service;

import com.fxrate.platform.producer.config.RateProducerProperties;
import com.fxrate.platform.rate.dto.RateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service that simulates live FX rates and publishes them periodically to RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final RateProducerProperties properties;

    @Value("${app.rabbitmq.rate-input-queue:rate.input.queue}")
    private String rateInputQueue;

    /**
     * Scheduled method to periodically publish simulated rates.
     */
    @Scheduled(fixedDelayString = "${app.producer.interval-ms:1000}")
    public void publishSimulatedRate() {
        if (!properties.isEnabled()) {
            return;
        }

        List<String> pairs = properties.getPairs();
        if (pairs == null) {
            return;
        }

        List<String> validPairs = pairs.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .map(p -> p.trim().toUpperCase(java.util.Locale.ROOT))
                .toList();

        if (validPairs.isEmpty()) {
            return;
        }

        // Pick a random pair
        String pair = validPairs.get(ThreadLocalRandom.current().nextInt(validPairs.size()));

        // Generate realistic random bid and ask
        BigDecimal bid = generateRandomBid(pair);
        BigDecimal spread = generateRandomSpread(pair);
        BigDecimal ask = bid.add(spread).setScale(4, RoundingMode.HALF_UP);

        RateMessage message = new RateMessage(
            "SIM",
            pair,
            bid,
            ask,
            System.currentTimeMillis()
        );

        rabbitTemplate.convertAndSend(rateInputQueue, message);
        log.info("[SIM_RATE_PUBLISHED] provider={} pair={} bid={} ask={} timestamp={}",
            message.provider(), message.pair(), message.bid(), message.ask(), message.timestamp());
    }

    private BigDecimal generateRandomBid(String pair) {
        double base = switch (pair) {
            case "EUR/USD" -> 1.08;
            case "USD/TRY" -> 32.50;
            case "GBP/USD" -> 1.27;
            default -> 1.0;
        };
        double fluctuation = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.01 * base;
        return BigDecimal.valueOf(base + fluctuation).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal generateRandomSpread(String pair) {
        double spread = switch (pair) {
            case "EUR/USD" -> 0.0002;
            case "USD/TRY" -> 0.05;
            case "GBP/USD" -> 0.0003;
            default -> 0.001;
        };
        double fluctuation = (ThreadLocalRandom.current().nextDouble() - 0.2) * 0.1 * spread;
        // Ensure spread is strictly positive
        double finalSpread = Math.max(0.0001, spread + fluctuation);
        return BigDecimal.valueOf(finalSpread).setScale(4, RoundingMode.HALF_UP);
    }
}
