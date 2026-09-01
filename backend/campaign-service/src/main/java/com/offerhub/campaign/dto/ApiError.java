package com.offerhub.campaign.dto;

/**
 * Error payload. code is UPPER_SNAKE_CASE from docs/ERROR-CODES.md;
 * message is for developers/logs, clients render their own text per code.
 */
public record ApiError(String code, String message) {
}
