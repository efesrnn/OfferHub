package com.offerhub.identity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String email;
    private String code;
    private String newPassword;
}
