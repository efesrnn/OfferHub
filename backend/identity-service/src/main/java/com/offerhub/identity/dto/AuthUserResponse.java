package com.offerhub.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthUserResponse {
    private String id;
    private String role;
    private List<String> specialties;
    private List<String> regions;
    private boolean mustChangePassword;
}