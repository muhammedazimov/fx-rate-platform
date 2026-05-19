package com.fxrate.platform.rate.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Represents a processed FX Rate update.
 * Implements Serializable for Hazelcast caching.
 */
public record Rate(
    String provider,
    String pair,
    BigDecimal bid,
    BigDecimal ask,
    BigDecimal spread,
    boolean alarm,
    Long timestamp,
    Long receivedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
