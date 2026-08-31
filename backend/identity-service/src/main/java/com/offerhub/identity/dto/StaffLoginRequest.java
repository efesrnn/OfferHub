package com.offerhub.identity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffLoginRequest {
    private String email;
    private String password;
}