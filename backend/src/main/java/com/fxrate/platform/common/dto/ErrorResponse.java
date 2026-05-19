package com.fxrate.platform.common.dto;

/**
 * Standard REST API error response.
 */
public record ErrorResponse(
    String code,
    String message
) {}
