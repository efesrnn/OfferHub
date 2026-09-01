package com.offerhub.campaign.dto;

/**
 * Standard response envelope (docs/API-CONTRACT.md section 0).
 * { "success": true,  "data": {...}, "error": null }
 * { "success": false, "data": null,  "error": { "code": "...", "message": "..." } }
 */
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** @param code from the ErrorCode catalog (docs/ERROR-CODES.md) */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }
}
