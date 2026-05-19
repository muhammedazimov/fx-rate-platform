package com.fxrate.platform.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxrate.platform.rate.dto.RateResponse;
import com.fxrate.platform.rate.model.Rate;
import com.fxrate.platform.websocket.dto.RateUpdateMessage;
import com.fxrate.platform.websocket.handler.RateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateWebSocketBroadcaster {

    private final RateWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * Broadcasts rate updates to all connected WebSocket sessions subscribed to the rate's currency pair.
     */
    public void broadcastRateUpdate(Rate rate) {
        if (rate == null) {
            return;
        }

        String pair = rate.pair();
        RateResponse response = RateResponse.from(rate);
        RateUpdateMessage updateMessage = RateUpdateMessage.rateUpdate(response);

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(updateMessage);
        } catch (IOException e) {
            log.error("Failed to serialize rate update message: {}", e.getMessage());
            return;
        }

        TextMessage textMessage = new TextMessage(jsonPayload);
        Map<WebSocketSession, Set<String>> subscriptions = webSocketHandler.getSessionSubscriptions();

        for (Map.Entry<WebSocketSession, Set<String>> entry : subscriptions.entrySet()) {
            WebSocketSession session = entry.getKey();
            Set<String> pairs = entry.getValue();

            if (pairs != null && pairs.contains(pair)) {
                if (!session.isOpen()) {
                    log.info("Removing closed session: {}", session.getId());
                    subscriptions.remove(session);
                    continue;
                }

                try {
                    session.sendMessage(textMessage);
                    log.debug("Sent rate update for {} to session {}", pair, session.getId());
                } catch (IOException e) {
                    log.warn("Failed to send rate update to session {}. Closing and removing. Error: {}", session.getId(), e.getMessage());
                    try {
                        session.close();
                    } catch (IOException ignored) {}
                    subscriptions.remove(session);
                }
            }
        }
    }
}
