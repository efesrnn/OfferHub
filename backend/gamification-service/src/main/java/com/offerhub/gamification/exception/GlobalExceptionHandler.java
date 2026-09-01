package com.offerhub.gamification.exception;

import com.offerhub.gamification.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every exception into the standard error envelope. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex) {
        return build(ex.getCode(), ex.getMessage());
    }

    /** Wrong query parameter type, e.g. ?period=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(ErrorCode.VALIDATION_ERROR, ex.getName() + " has an invalid value");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return build(ErrorCode.NOT_FOUND, "No endpoint for this path");
    }

    /** Last resort - log the detail, never leak a stack trace to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, "Unexpected error");
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code.name(), message));
    }
}
