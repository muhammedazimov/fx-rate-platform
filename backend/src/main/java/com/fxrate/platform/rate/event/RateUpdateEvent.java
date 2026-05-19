package com.fxrate.platform.rate.event;

import com.fxrate.platform.rate.model.Rate;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Represents a rate update event published to Hazelcast Topic.
 */
public record RateUpdateEvent(
    String provider,
    String pair,
    BigDecimal bid,
    BigDecimal ask,
    BigDecimal spread,
    boolean alarm,
    Long timestamp,
    Long receivedAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static RateUpdateEvent from(Rate rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Rate cannot be null");
        }
        return new RateUpdateEvent(
            rate.provider(),
            rate.pair(),
            rate.bid(),
            rate.ask(),
            rate.spread(),
            rate.alarm(),
            rate.timestamp(),
            rate.receivedAt()
        );
    }

    public Rate toRate() {
        return new Rate(
            provider,
            pair,
            bid,
            ask,
            spread,
            alarm,
            timestamp,
            receivedAt
        );
    }
}
