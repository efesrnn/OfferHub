package com.offerhub.identity.controller;

import com.offerhub.identity.dto.ApiResponse;
import com.offerhub.identity.dto.StaffResponse;
import com.offerhub.identity.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Personel dizini - /api/v1/admin/** ROLE_ADMIN'e kilitli oldugu icin, Supervisor'in
 * case atarken uzman aramasi gibi admin olmayan ihtiyaclar icin ayri, dar kapsamli bir yol.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class StaffDirectoryController {

    private final AdminService adminService;

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<StaffResponse>>> searchStaff(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.success(adminService.searchStaff(query, role)));
    }
}
