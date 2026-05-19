package com.fxrate.platform.websocket.dto;

import java.util.List;

/**
 * Incoming subscription message from a WebSocket client.
 */
public record RateSubscriptionMessage(
    String type,
    List<String> pairs
) {}
