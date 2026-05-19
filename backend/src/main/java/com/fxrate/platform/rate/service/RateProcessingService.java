package com.fxrate.platform.rate.service;

import com.fxrate.platform.rate.dto.RateMessage;
import com.fxrate.platform.rate.model.Rate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Locale;

@Service
public class RateProcessingService {

    private final BigDecimal spreadAlarmThreshold;

    public RateProcessingService(
            @Value("${app.rate.spread-alarm-threshold:0.0010}") BigDecimal spreadAlarmThreshold) {
        this.spreadAlarmThreshold = spreadAlarmThreshold;
    }

    /**
     * Processes a RateMessage to compute spread and alarm, then outputs a Rate record.
     */
    public Rate process(RateMessage message) {
        BigDecimal spread = message.ask().subtract(message.bid());
        boolean alarm = spread.compareTo(spreadAlarmThreshold) > 0;
        long receivedAt = System.currentTimeMillis();
        String normalizedPair = message.pair().trim().toUpperCase(Locale.ROOT);

        return new Rate(
                message.provider(),
                normalizedPair,
                message.bid(),
                message.ask(),
                spread,
                alarm,
                message.timestamp(),
                receivedAt
        );
    }
}
