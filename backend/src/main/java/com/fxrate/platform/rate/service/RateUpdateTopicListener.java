package com.fxrate.platform.rate.service;

import com.fxrate.platform.rate.config.RateTopicNames;
import com.fxrate.platform.rate.event.RateUpdateEvent;
import com.fxrate.platform.websocket.service.RateWebSocketBroadcaster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listener that subscribes to Hazelcast Topic and broadcasts rate updates to local WebSocket clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateUpdateTopicListener implements MessageListener<RateUpdateEvent> {

    private final HazelcastInstance hazelcastInstance;
    private final RateWebSocketBroadcaster rateWebSocketBroadcaster;

    private UUID registrationId;

    @PostConstruct
    public void init() {
        ITopic<RateUpdateEvent> topic = hazelcastInstance.getTopic(RateTopicNames.RATE_UPDATES_TOPIC);
        registrationId = topic.addMessageListener(this);
        log.info("Subscribed to Hazelcast Topic '{}' with registration ID: {}", RateTopicNames.RATE_UPDATES_TOPIC, registrationId);
    }

    @Override
    public void onMessage(Message<RateUpdateEvent> message) {
        RateUpdateEvent event = message.getMessageObject();
        if (event == null) {
            log.warn("Received null rate update event from Hazelcast Topic");
            return;
        }
        log.info("[RATE_TOPIC_RECEIVED] pair={} timestamp={}", event.pair(), event.timestamp());
        rateWebSocketBroadcaster.broadcastRateUpdate(event.toRate());
    }

    @PreDestroy
    public void destroy() {
        if (registrationId != null) {
            try {
                ITopic<RateUpdateEvent> topic = hazelcastInstance.getTopic(RateTopicNames.RATE_UPDATES_TOPIC);
                topic.removeMessageListener(registrationId);
                log.info("Unsubscribed from Hazelcast Topic '{}' for registration ID: {}", RateTopicNames.RATE_UPDATES_TOPIC, registrationId);
            } catch (Exception e) {
                log.warn("Failed to remove Hazelcast Topic listener", e);
            }
        }
    }
}
