package com.offerhub.campaign.controller;

import com.offerhub.campaign.dto.ApiResponse;
import com.offerhub.campaign.dto.CampaignResponse;
import com.offerhub.campaign.dto.CreateCampaignRequest;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.security.CallerIdentity;
import com.offerhub.campaign.security.Role;
import com.offerhub.campaign.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    /** Caps how much one request can pull, whatever size the caller asks for. */
    private static final int MAX_PAGE_SIZE = 100;

    private final CampaignService campaignService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CampaignResponse> create(@Valid @RequestBody CreateCampaignRequest request,
                                                CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(campaignService.create(request));
    }

    @GetMapping
    public ApiResponse<PagedResult<CampaignResponse>> list(
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(required = false) Segment segment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            CallerIdentity caller) {

        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR, Role.ADMIN);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return ApiResponse.ok(campaignService.list(status, segment, pageable));
    }

    @GetMapping("/{campaignNo}")
    public ApiResponse<CampaignResponse> get(@PathVariable String campaignNo, CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR, Role.ADMIN);
        return ApiResponse.ok(campaignService.getByCampaignNo(campaignNo));
    }
}