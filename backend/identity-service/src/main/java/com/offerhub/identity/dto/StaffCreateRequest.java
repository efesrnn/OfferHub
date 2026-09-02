package com.offerhub.identity.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StaffCreateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private List<String> specialties;
    private List<String> regions;
}
