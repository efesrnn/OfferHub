package com.offerhub.identity.controller;

import com.offerhub.identity.dto.ApiResponse;
import com.offerhub.identity.dto.StaffCreateRequest;
import com.offerhub.identity.dto.StaffCreateResponse;
import com.offerhub.identity.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<StaffCreateResponse>> createStaff(@RequestBody StaffCreateRequest request) {
        StaffCreateResponse response = adminService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
