package com.offerhub.campaign.controller;

import com.offerhub.campaign.dto.ApiResponse;
import com.offerhub.campaign.dto.CampaignResponse;
import com.offerhub.campaign.dto.CreateCampaignRequest;
import com.offerhub.campaign.dto.DashboardResponse;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.dto.ClassificationRequest;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.security.CallerIdentity;
import com.offerhub.campaign.security.Role;
import com.offerhub.campaign.service.CampaignService;
import com.offerhub.campaign.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Campaigns", description = "Campaign lifecycle, AI classification and the supervisor dashboard")
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    /** Caps how much one request can pull, whatever size the caller asks for. */
    private static final int MAX_PAGE_SIZE = 100;

    private final CampaignService campaignService;
    private final DashboardService dashboardService;

    @Operation(summary = "Create a campaign",
            description = """
                    Targeting a segment sends the campaign to AI for a conversion estimate, a
                    classification and a priority. AI being unreachable does not fail the request:
                    the campaign is still created, unscored, as BELIRSIZ with priority ORTA, and
                    goes to the manual optimization queue.

                    A campaign AI scores below 0.60 opens an optimization case and waits in YENI.
                    One scored at or above 0.60 has no case and therefore nobody to wait for, so
                    it starts in YAYINDA.

                    Roles: EXPERT, SUPERVISOR.""")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CampaignResponse> create(@Valid @RequestBody CreateCampaignRequest request,
                                                CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(campaignService.create(request, caller));
    }

    @Operation(summary = "List campaigns",
            description = """
                    Both filters are optional. size is clamped to 100 and a negative page becomes 0.

                    An expert sees only campaigns they created or whose optimization case is
                    assigned to them. Supervisors and admins see all of them.

                    Roles: EXPERT, SUPERVISOR, ADMIN.""")
    @GetMapping
    public ApiResponse<PagedResult<CampaignResponse>> list(
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(required = false) Segment segment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            CallerIdentity caller) {

        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR, Role.ADMIN);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return ApiResponse.ok(campaignService.list(status, segment, caller, pageable));
    }

    /**
     * Declared before /{campaignNo} on purpose is not enough on its own - Spring prefers
     * the literal path over the variable one, so "dashboard" is never read as a campaign
     * number. Kept adjacent so the reason stays visible.
     */
    @Operation(summary = "Supervisor dashboard",
            description = """
                    Every card section 8.1 asks for: segment distribution, conversion rate and its
                    daily trend, SLA compliance with the count of breached active cases, AI accuracy
                    together with the number of classifications behind it, per expert performance,
                    and the pending optimization queue.

                    aiAccuracyRate starts at 1.00 because a campaign nobody has corrected counts as
                    correctly classified, which is why aiClassifiedCampaigns is sent alongside it as
                    the denominator.

                    Roles: SUPERVISOR, ADMIN.""")
    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard(CallerIdentity caller) {
        caller.requireAnyOf(Role.SUPERVISOR, Role.ADMIN);
        return ApiResponse.ok(dashboardService.load());
    }

    /**
     * AI override - segment, type or priority. Admins are out: the role matrix gives this
     * to the two who do the work. Priority is narrowed to supervisors inside the service,
     * because that rule depends on the body rather than on the endpoint.
     */
    @Operation(summary = "Correct the AI classification",
            description = """
                    Overrides segment, type or priority. At least one of the three is required.

                    A changed segment is published as a misclassification, which is what the AI
                    accuracy metric is built from. aiSegment itself is never overwritten: it is the
                    baseline accuracy is measured against, so erasing it would erase the mistake
                    this endpoint exists to record. Setting a value it already has changes nothing
                    and reports nothing, so a retry is safe.

                    Priority is supervisor only. Moving a segment to RISKLI_KAYIP raises priority to
                    at least YUKSEK and moves the SLA deadline with it.

                    Roles: EXPERT, SUPERVISOR.""")
    @PatchMapping("/{campaignNo}/classification")
    public ApiResponse<CampaignResponse> reclassify(@PathVariable String campaignNo,
                                                    @Valid @RequestBody ClassificationRequest request,
                                                    CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(campaignService.reclassify(campaignNo, request, caller));
    }

    @Operation(summary = "Get one campaign",
            description = """
                    Addressed by campaignNo, such as CMP-2026-000123, rather than by the internal id.

                    Roles: EXPERT, SUPERVISOR, ADMIN.""")
    @GetMapping("/{campaignNo}")
    public ApiResponse<CampaignResponse> get(@PathVariable String campaignNo, CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR, Role.ADMIN);
        return ApiResponse.ok(campaignService.getByCampaignNo(campaignNo));
    }
}
