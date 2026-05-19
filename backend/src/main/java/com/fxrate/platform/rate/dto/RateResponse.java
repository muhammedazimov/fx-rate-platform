package com.fxrate.platform.rate.dto;

import com.fxrate.platform.rate.model.Rate;
import java.math.BigDecimal;

/**
 * REST API response DTO for FX Rate snapshot.
 */
public record RateResponse(
    String provider,
    String pair,
    BigDecimal bid,
    BigDecimal ask,
    BigDecimal spread,
    boolean alarm,
    Long timestamp,
    Long receivedAt
) {
    public static RateResponse from(Rate rate) {
        if (rate == null) {
            return null;
        }
        return new RateResponse(
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
}
