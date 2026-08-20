package com.offerhub.campaign.exception;

import com.offerhub.campaign.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/** Turns every exception into the standard error envelope. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex) {
        return build(ex.getCode(), ex.getMessage());
    }

    /** Bean validation failure - the message names the offending field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(ErrorCode.VALIDATION_ERROR, message);
    }

    /** Malformed JSON or an unknown enum value - a client mistake, not a server error. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.VALIDATION_ERROR, "Request body could not be read");
    }

    /** Wrong query parameter type, e.g. ?page=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(ErrorCode.VALIDATION_ERROR, ex.getName() + " has an invalid value");
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
