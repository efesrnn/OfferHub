package com.offerhub.identity.dto;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ApiError error;

    private ApiResponse(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }

    public static <T> ApiResponse<T> error(String code, String message, java.time.Instant lockedUntil) {
        String lockedUntilStr = lockedUntil != null ? lockedUntil.toString() : null;
        return new ApiResponse<>(false, null, new ApiError(code, message, lockedUntilStr));
    }
}