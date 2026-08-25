package com.offerhub.identity.controller;

import com.offerhub.identity.dto.*;
import com.offerhub.identity.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/otp-verify")
    public ResponseEntity<ApiResponse<AuthDataResponse>> verifyOtp(@RequestBody OtpVerifyRequest request) {
        AuthDataResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDataResponse>> staffLogin(@RequestBody StaffLoginRequest request) {
        AuthDataResponse response = authService.staffLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}