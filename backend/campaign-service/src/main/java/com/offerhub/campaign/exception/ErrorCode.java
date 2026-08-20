package com.offerhub.campaign.exception;

import org.springframework.http.HttpStatus;

/**
 * Error catalog from docs/ERROR-CODES.md.
 * The enum name is what goes into the JSON error.code field.
 */
public enum ErrorCode {

    // shared by all services
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    // campaign service
    INVALID_STATE_TRANSITION(HttpStatus.UNPROCESSABLE_CONTENT),
    OPTIMIZATION_NOTE_REQUIRED(HttpStatus.BAD_REQUEST),
    OFFER_ALREADY_RATED(HttpStatus.CONFLICT),
    OFFER_ALREADY_RESPONDED(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
