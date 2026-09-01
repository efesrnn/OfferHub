package com.offerhub.identity.exception;

import com.offerhub.identity.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResource(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_RESOURCE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOtp(InvalidOtpException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("INVALID_OTP", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        log.error("Beklenmeyen hata:", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "Beklenmeyen bir hata olustu"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.error("ACCOUNT_LOCKED", ex.getMessage(), ex.getLockedUntil()));
    }
}