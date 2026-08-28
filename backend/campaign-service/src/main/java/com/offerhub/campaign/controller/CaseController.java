package com.offerhub.campaign.controller;

import com.offerhub.campaign.dto.ApiResponse;
import com.offerhub.campaign.dto.AssignRequest;
import com.offerhub.campaign.dto.CaseResponse;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.dto.StatusChangeRequest;
import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.service.OptimizationCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    /** Caps how much one request can pull, whatever size the caller asks for. */
    private static final int MAX_PAGE_SIZE = 100;

    private final OptimizationCaseService caseService;

    /**
     * The list is always priority ordered, so no sort parameter yet - sort=sla arrives
     * with the SLA round.
     * TODO: assignedTo=me should resolve to the caller once JWT roles land.
     */
    @GetMapping
    public ApiResponse<PagedResult<CaseResponse>> list(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return ApiResponse.ok(caseService.list(status, assignedTo, pageable));
    }

    @GetMapping("/{caseId}")
    public ApiResponse<CaseResponse> get(@PathVariable UUID caseId) {
        return ApiResponse.ok(caseService.getById(caseId));
    }

    @PostMapping("/{caseId}/assign")
    public ApiResponse<CaseResponse> assign(@PathVariable UUID caseId,
                                            @Valid @RequestBody AssignRequest request) {
        return ApiResponse.ok(caseService.assign(caseId, request));
    }

    @PatchMapping("/{caseId}/status")
    public ApiResponse<CaseResponse> changeStatus(@PathVariable UUID caseId,
                                                  @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok(caseService.changeStatus(caseId, request));
    }
}
