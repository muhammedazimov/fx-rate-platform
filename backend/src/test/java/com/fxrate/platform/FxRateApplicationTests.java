package com.fxrate.platform;

import com.fxrate.platform.rate.dto.RateMessage;
import com.fxrate.platform.rate.model.Rate;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FxRateApplicationTests {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fxrate.platform.websocket.handler.RateWebSocketHandler webSocketHandler;

    @Autowired
    private com.fxrate.platform.websocket.service.RateWebSocketBroadcaster webSocketBroadcaster;

    @Value("${app.rabbitmq.rate-input-queue:rate.input.queue}")
    private String queueName;

    @Test
    void testRateProcessingCachingAndRestEndpoints() throws Exception {
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

        // 5. Test REST Endpoint: GET /api/rates
        mockMvc.perform(get("/api/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].pair").value("EUR/USD"))
                .andExpect(jsonPath("$[0].provider").value("LP1"))
                .andExpect(jsonPath("$[0].bid").value(1.0850))
                .andExpect(jsonPath("$[0].ask").value(1.0853))
                .andExpect(jsonPath("$[0].spread").value(0.0003))
                .andExpect(jsonPath("$[0].alarm").value(false))
                .andExpect(jsonPath("$[0].timestamp").value(1710000000999L));

        // 6. Test REST Endpoint: GET /api/rates/EUR/USD (exact match)
        mockMvc.perform(get("/api/rates/EUR/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pair").value("EUR/USD"))
                .andExpect(jsonPath("$.timestamp").value(1710000000999L));

        // 7. Test REST Endpoint: GET /api/rates/eur/usd (case-insensitive path variables mapping/normalization)
        mockMvc.perform(get("/api/rates/eur/usd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pair").value("EUR/USD"))
                .andExpect(jsonPath("$.timestamp").value(1710000000999L));

        // 8. Test REST Endpoint: GET /api/rates/USD/TRY (non-existent, should return 404 with ErrorResponse)
        mockMvc.perform(get("/api/rates/USD/TRY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RATE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No rate found for pair USD/TRY"));
    }

    @Test
    void testWebSocketSubscriptionAndBroadcast() throws Exception {
        org.springframework.web.socket.WebSocketSession session = org.mockito.Mockito.mock(org.springframework.web.socket.WebSocketSession.class);
        org.mockito.Mockito.when(session.getId()).thenReturn("test-session-123");
        org.mockito.Mockito.when(session.isOpen()).thenReturn(true);

        webSocketHandler.afterConnectionEstablished(session);
        assertThat(webSocketHandler.getSessionSubscriptions()).containsKey(session);

        String subscribeJson = "{\"type\":\"SUBSCRIBE\",\"pairs\":[\"EUR/USD\"]}";
        webSocketHandler.handleMessage(session, new org.springframework.web.socket.TextMessage(subscribeJson));

        assertThat(webSocketHandler.getSessionSubscriptions().get(session)).contains("EUR/USD");
        org.mockito.Mockito.verify(session).sendMessage(org.mockito.Mockito.any(org.springframework.web.socket.TextMessage.class));

        org.mockito.Mockito.reset(session);
        org.mockito.Mockito.when(session.isOpen()).thenReturn(true);

        Rate rate = new Rate("LP1", "EUR/USD", new BigDecimal("1.0850"), new BigDecimal("1.0853"), new BigDecimal("0.0003"), false, 1710000000999L, System.currentTimeMillis());
        webSocketBroadcaster.broadcastRateUpdate(rate);

        org.mockito.Mockito.verify(session).sendMessage(org.mockito.Mockito.argThat(msg -> {
            if (msg instanceof org.springframework.web.socket.TextMessage) {
                String payload = ((org.springframework.web.socket.TextMessage) msg).getPayload();
                return payload.contains("RATE_UPDATE") && payload.contains("EUR/USD") && payload.contains("1.0850");
            }
            return false;
        }));

        webSocketHandler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);
        assertThat(webSocketHandler.getSessionSubscriptions()).doesNotContainKey(session);
    }

    @Test
    void testHazelcastTopicSubscriptionAndBroadcast() throws Exception {
        org.springframework.web.socket.WebSocketSession session = org.mockito.Mockito.mock(org.springframework.web.socket.WebSocketSession.class);
        org.mockito.Mockito.when(session.getId()).thenReturn("topic-session-123");
        org.mockito.Mockito.when(session.isOpen()).thenReturn(true);

        webSocketHandler.afterConnectionEstablished(session);
        String subscribeJson = "{\"type\":\"SUBSCRIBE\",\"pairs\":[\"EUR/USD\"]}";
        webSocketHandler.handleMessage(session, new org.springframework.web.socket.TextMessage(subscribeJson));

        org.mockito.Mockito.reset(session);
        org.mockito.Mockito.when(session.isOpen()).thenReturn(true);

        com.hazelcast.topic.ITopic<com.fxrate.platform.rate.event.RateUpdateEvent> topic =
                hazelcastInstance.getTopic(com.fxrate.platform.rate.config.RateTopicNames.RATE_UPDATES_TOPIC);

        com.fxrate.platform.rate.event.RateUpdateEvent event = new com.fxrate.platform.rate.event.RateUpdateEvent(
                "LP1", "EUR/USD", new BigDecimal("1.0855"), new BigDecimal("1.0858"), new BigDecimal("0.0003"), false, 1710000002000L, System.currentTimeMillis()
        );
        topic.publish(event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            org.mockito.Mockito.verify(session).sendMessage(org.mockito.Mockito.argThat(msg -> {
                if (msg instanceof org.springframework.web.socket.TextMessage) {
                    String payload = ((org.springframework.web.socket.TextMessage) msg).getPayload();
                    return payload.contains("RATE_UPDATE") && payload.contains("EUR/USD") && payload.contains("1.0855");
                }
                return false;
            }));
        });

        webSocketHandler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);
    }
}
