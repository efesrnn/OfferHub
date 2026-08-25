package com.offerhub.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthDataResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private AuthUserResponse user;
}