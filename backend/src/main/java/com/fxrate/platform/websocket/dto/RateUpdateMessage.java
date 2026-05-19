package com.fxrate.platform.websocket.dto;

import com.fxrate.platform.rate.dto.RateResponse;

/**
 * Outgoing live rate update payload for WebSocket clients.
 */
public record RateUpdateMessage(
    String type,
    RateResponse data
) {
    public static RateUpdateMessage rateUpdate(RateResponse response) {
        return new RateUpdateMessage("RATE_UPDATE", response);
    }
}
