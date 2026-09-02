package com.offerhub.identity.controller;

import com.offerhub.identity.dto.ApiResponse;
import com.offerhub.identity.dto.AuditLogResponse;
import com.offerhub.identity.dto.PagedResponse;
import com.offerhub.identity.dto.StaffCreateRequest;
import com.offerhub.identity.dto.StaffCreateResponse;
import com.offerhub.identity.security.ClientIpResolver;
import com.offerhub.identity.service.AdminService;
import com.offerhub.identity.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<StaffCreateResponse>> createStaff(@RequestBody StaffCreateRequest request,
                                                                          HttpServletRequest httpRequest) {
        StaffCreateResponse response = adminService.createStaff(request, ClientIpResolver.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String actionQuery,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AuditLogResponse> response =
                auditLogService.search(actionQuery, action, result, fromDate, toDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
