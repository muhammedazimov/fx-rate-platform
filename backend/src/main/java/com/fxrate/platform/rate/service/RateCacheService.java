package com.fxrate.platform.rate.service;

import com.fxrate.platform.rate.model.Rate;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateCacheService {

    private final HazelcastInstance hazelcastInstance;

    /**
     * Updates the cache with the incoming rate if it is newer than the cached rate for the pair.
     * Uses pair-level locking to avoid race conditions.
     */
    public boolean updateCache(Rate incomingRate) {
        String pair = incomingRate.pair();
        IMap<String, Rate> ratesMap = hazelcastInstance.getMap("rates");

        ratesMap.lock(pair);
        try {
            Rate currentRate = ratesMap.get(pair);
            if (currentRate == null || incomingRate.timestamp() > currentRate.timestamp()) {
                ratesMap.put(pair, incomingRate);
                log.info("[RATE_UPDATED] pair={} provider={} bid={} ask={} spread={} alarm={} timestamp={}",
                        incomingRate.pair(), incomingRate.provider(), incomingRate.bid(), incomingRate.ask(),
                        incomingRate.spread(), incomingRate.alarm(), incomingRate.timestamp());
                return true;
            } else {
                log.info("[RATE_STALE_IGNORED] pair={} incomingTimestamp={} currentTimestamp={}",
                        incomingRate.pair(), incomingRate.timestamp(), currentRate.timestamp());
                return false;
            }
        } finally {
            ratesMap.unlock(pair);
        }
    }
}
