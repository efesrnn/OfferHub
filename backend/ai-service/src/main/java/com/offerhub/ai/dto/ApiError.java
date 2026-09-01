package com.offerhub.ai.dto;

import lombok.Getter;

@Getter
public class ApiError {
    private final String code;
    private final String message;
    private final String lockedUntil;

    public ApiError(String code, String message) {
        this(code, message, null);
    }

    public ApiError(String code, String message, String lockedUntil) {
        this.code = code;
        this.message = message;
        this.lockedUntil = lockedUntil;
    }
}
