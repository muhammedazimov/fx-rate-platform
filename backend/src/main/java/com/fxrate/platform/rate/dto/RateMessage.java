package com.fxrate.platform.rate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RateMessage(
    @NotBlank String provider,
    @NotBlank String pair,
    @NotNull @Positive BigDecimal bid,
    @NotNull @Positive BigDecimal ask,
    @NotNull @Positive Long timestamp
) {}
