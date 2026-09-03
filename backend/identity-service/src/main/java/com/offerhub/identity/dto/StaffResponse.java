package com.offerhub.identity.dto;

import com.offerhub.identity.entity.StaffUser;

import java.util.List;

public record StaffResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String role,
        List<String> specialties,
        List<String> regions
) {
    public static StaffResponse from(StaffUser staff) {
        return new StaffResponse(
                staff.getId().toString(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getEmail(),
                staff.getRole().name(),
                staff.getSpecialties(),
                staff.getRegions());
    }
}
