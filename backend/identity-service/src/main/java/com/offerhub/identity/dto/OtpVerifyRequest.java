package com.offerhub.identity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequest {
    private AuthMode authMode;
    private String phone;
    private String credential;
}