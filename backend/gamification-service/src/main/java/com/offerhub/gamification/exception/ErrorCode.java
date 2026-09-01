package com.offerhub.gamification.exception;

import org.springframework.http.HttpStatus;

/** Error catalog from docs/ERROR-CODES.md. Gamification is read-only to clients, so the list is short. */
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
