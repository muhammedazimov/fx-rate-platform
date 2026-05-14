package com.fxrate.platform.rate.service;

public record ValidationResult(boolean isValid, String reason) {
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String reason) {
        return new ValidationResult(false, reason);
    }
}
