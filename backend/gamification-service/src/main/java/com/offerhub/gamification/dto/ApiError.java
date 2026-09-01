package com.offerhub.gamification.dto;

/** Error payload; code is UPPER_SNAKE_CASE from docs/ERROR-CODES.md. */
public record ApiError(String code, String message) {
}
