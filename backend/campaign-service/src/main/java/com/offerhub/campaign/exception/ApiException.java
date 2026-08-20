package com.offerhub.campaign.exception;

import lombok.Getter;

/** Business exception carrying an ErrorCode; turned into the error envelope by GlobalExceptionHandler. */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
