package com.fxrate.platform.rate.consumer;

import com.fxrate.platform.rate.dto.RateMessage;
import com.fxrate.platform.rate.model.Rate;
import com.fxrate.platform.rate.service.RateCacheService;
import com.fxrate.platform.rate.service.RateProcessingService;
import com.fxrate.platform.rate.service.RateValidationService;
import com.fxrate.platform.rate.service.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateConsumer {

    private final RateValidationService validationService;
    private final RateProcessingService rateProcessingService;
    private final RateCacheService rateCacheService;

    @RabbitListener(queues = "${app.rabbitmq.rate-input-queue:rate.input.queue}")
    public void consumeRate(RateMessage message) {
        ValidationResult result = validationService.validate(message);

        if (result.isValid()) {
            Rate processedRate = rateProcessingService.process(message);
            rateCacheService.updateCache(processedRate);
        } else {
            log.warn("[RATE_REJECTED] reason={} payload={}", result.reason(), message);
        }
    }
}
