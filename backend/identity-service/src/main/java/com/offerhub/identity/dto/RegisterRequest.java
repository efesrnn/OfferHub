package com.offerhub.identity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private AuthMode authMode;
}