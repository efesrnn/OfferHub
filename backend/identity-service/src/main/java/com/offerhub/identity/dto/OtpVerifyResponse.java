package com.offerhub.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OtpVerifyResponse {
    private String subscriberId;
    private String firstName;
    private String phone;
    // TODO: JWT  accessToken, refreshToken, expiresIn buraya eklenecek
}