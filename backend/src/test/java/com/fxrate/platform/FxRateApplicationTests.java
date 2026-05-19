package com.fxrate.platform;

import com.fxrate.platform.rate.dto.RateMessage;
import com.fxrate.platform.rate.model.Rate;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class FxRateApplicationTests {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Value("${app.rabbitmq.rate-input-queue:rate.input.queue}")
    private String queueName;

    @Test
    void testRateProcessingAndCaching() throws Exception {
        IMap<String, Rate> ratesMap = hazelcastInstance.getMap("rates");
        ratesMap.clear();

        // 1. Send valid rate message
        RateMessage message1 = new RateMessage("LP1", "EUR/USD", new BigDecimal("1.0845"), new BigDecimal("1.0847"), 1710000000123L);
        rabbitTemplate.convertAndSend(queueName, message1);

        // Await until stored in Hazelcast
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Rate rate = ratesMap.get("EUR/USD");
            assertThat(rate).isNotNull();
            assertThat(rate.provider()).isEqualTo("LP1");
            assertThat(rate.bid()).isEqualByComparingTo("1.0845");
            assertThat(rate.ask()).isEqualByComparingTo("1.0847");
            assertThat(rate.spread()).isEqualByComparingTo("0.0002");
            assertThat(rate.alarm()).isFalse();
            assertThat(rate.timestamp()).isEqualTo(1710000000123L);
        });

        // 2. Send an older rate message (should be ignored)
        RateMessage message2 = new RateMessage("LP1", "EUR/USD", new BigDecimal("1.0840"), new BigDecimal("1.0842"), 1710000000000L);
        rabbitTemplate.convertAndSend(queueName, message2);

        // Wait a bit and check that it wasn't overwritten
        Thread.sleep(1000);
        Rate rateAfterOlder = ratesMap.get("EUR/USD");
        assertThat(rateAfterOlder.timestamp()).isEqualTo(1710000000123L);

        // 3. Send a newer rate message (should update)
        RateMessage message3 = new RateMessage("LP1", "EUR/USD", new BigDecimal("1.0850"), new BigDecimal("1.0853"), 1710000000999L);
        rabbitTemplate.convertAndSend(queueName, message3);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Rate rate = ratesMap.get("EUR/USD");
            assertThat(rate).isNotNull();
            assertThat(rate.timestamp()).isEqualTo(1710000000999L);
            assertThat(rate.bid()).isEqualByComparingTo("1.0850");
            assertThat(rate.ask()).isEqualByComparingTo("1.0853");
            assertThat(rate.spread()).isEqualByComparingTo("0.0003");
        });

        // 4. Send an invalid rate message (should be rejected and not update the cache)
        RateMessage message4 = new RateMessage("LP1", "EUR/USD", new BigDecimal("1.0860"), new BigDecimal("1.0840"), 1710000099999L);
        rabbitTemplate.convertAndSend(queueName, message4);

        // Wait a bit and check that it wasn't updated to the invalid rate despite having a newer timestamp
        Thread.sleep(1000);
        Rate rateAfterInvalid = ratesMap.get("EUR/USD");
        assertThat(rateAfterInvalid.timestamp()).isEqualTo(1710000000999L);
        assertThat(rateAfterInvalid.bid()).isEqualByComparingTo("1.0850");
    }
}
