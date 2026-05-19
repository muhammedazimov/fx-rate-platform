package com.fxrate.platform.rate.service;

import com.fxrate.platform.rate.dto.RateResponse;
import com.fxrate.platform.rate.model.Rate;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RateQueryService {

    private final HazelcastInstance hazelcastInstance;

    /**
     * Retrieves all cached rates sorted alphabetically by pair.
     */
    public List<RateResponse> getAllRates() {
        IMap<String, Rate> ratesMap = hazelcastInstance.getMap("rates");
        return ratesMap.values().stream()
                .map(RateResponse::from)
                .sorted(Comparator.comparing(RateResponse::pair))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the cached rate for a given pair.
     */
    public Optional<RateResponse> getRateByPair(String pair) {
        IMap<String, Rate> ratesMap = hazelcastInstance.getMap("rates");
        Rate rate = ratesMap.get(pair);
        return Optional.ofNullable(rate).map(RateResponse::from);
    }
}
