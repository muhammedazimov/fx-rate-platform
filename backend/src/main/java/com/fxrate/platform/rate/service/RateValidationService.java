package com.fxrate.platform.rate.service;

import com.fxrate.platform.rate.dto.RateMessage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RateValidationService {

    public ValidationResult validate(RateMessage message) {
        if (message == null) {
            return ValidationResult.invalid("MESSAGE_NULL");
        }
        if (message.provider() == null || message.provider().isBlank()) {
            return ValidationResult.invalid("PROVIDER_BLANK");
        }
        if (message.pair() == null || message.pair().isBlank()) {
            return ValidationResult.invalid("PAIR_BLANK");
        }
        if (message.bid() == null) {
            return ValidationResult.invalid("BID_NULL");
        }
        if (message.ask() == null) {
            return ValidationResult.invalid("ASK_NULL");
        }
        if (message.timestamp() == null) {
            return ValidationResult.invalid("TIMESTAMP_NULL");
        }
        if (message.bid().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("BID_NOT_POSITIVE");
        }
        if (message.ask().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("ASK_NOT_POSITIVE");
        }
        if (message.ask().compareTo(message.bid()) < 0) {
            return ValidationResult.invalid("ASK_LESS_THAN_BID");
        }
        if (message.timestamp() <= 0) {
            return ValidationResult.invalid("TIMESTAMP_NOT_POSITIVE");
        }

        return ValidationResult.valid();
    }
}
