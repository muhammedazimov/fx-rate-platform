package com.fxrate.platform.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxrate.platform.websocket.dto.RateSubscriptionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<WebSocketSession, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    public Map<WebSocketSession, Set<String>> getSessionSubscriptions() {
        return sessionSubscriptions;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
        sessionSubscriptions.put(session, ConcurrentHashMap.newKeySet());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            RateSubscriptionMessage subMessage = objectMapper.readValue(message.getPayload(), RateSubscriptionMessage.class);
            if (subMessage != null && "SUBSCRIBE".equalsIgnoreCase(subMessage.type()) && subMessage.pairs() != null) {
                
                Set<String> subs = sessionSubscriptions.computeIfAbsent(session, k -> ConcurrentHashMap.newKeySet());
                List<String> normalizedPairs = subMessage.pairs().stream()
                        .filter(p -> p != null && !p.trim().isEmpty())
                        .map(p -> p.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toList());
                
                subs.addAll(normalizedPairs);
                
                log.info("Session {} subscribed to pairs: {}", session.getId(), normalizedPairs);

                Map<String, Object> ack = new HashMap<>();
                ack.put("type", "SUBSCRIBED");
                ack.put("pairs", normalizedPairs);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
            } else {
                sendError(session, "Invalid subscription message");
            }
        } catch (Exception e) {
            log.warn("Error processing WebSocket message from session {}: {}", session.getId(), e.getMessage());
            sendError(session, "Invalid subscription message");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection closed: {} (status: {})", session.getId(), status);
        sessionSubscriptions.remove(session);
    }

    private void sendError(WebSocketSession session, String errorMsg) throws IOException {
        if (session.isOpen()) {
            Map<String, String> err = new HashMap<>();
            err.put("type", "ERROR");
            err.put("message", errorMsg);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(err)));
        }
    }
}
