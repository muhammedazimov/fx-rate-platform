package com.fxrate.platform.rate.service;

import com.fxrate.platform.rate.config.RateTopicNames;
import com.fxrate.platform.rate.event.RateUpdateEvent;
import com.fxrate.platform.rate.model.Rate;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to publish rate updates to Hazelcast Topic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateUpdatePublisher {

    private final HazelcastInstance hazelcastInstance;

    public void publish(Rate rate) {
        ITopic<RateUpdateEvent> topic = hazelcastInstance.getTopic(RateTopicNames.RATE_UPDATES_TOPIC);
        RateUpdateEvent event = RateUpdateEvent.from(rate);
        topic.publish(event);
        log.info("[RATE_TOPIC_PUBLISHED] pair={} timestamp={}", rate.pair(), rate.timestamp());
    }
}
