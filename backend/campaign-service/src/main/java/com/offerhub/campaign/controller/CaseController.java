package com.offerhub.campaign.controller;

import com.offerhub.campaign.dto.ApiResponse;
import com.offerhub.campaign.dto.AssignRequest;
import com.offerhub.campaign.dto.CaseResponse;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.dto.StatusChangeRequest;
import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.exception.ApiException;
import com.offerhub.campaign.exception.ErrorCode;
import com.offerhub.campaign.security.CallerIdentity;
import com.offerhub.campaign.security.Role;
import com.offerhub.campaign.service.CaseSort;
import com.offerhub.campaign.service.OptimizationCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * The documented responses spell out io.swagger...ApiResponse in full: this project already
 * has an ApiResponse of its own, the response envelope imported above, so the short name is
 * taken.
 */
@RestController
@Tag(name = "Optimization cases", description = "The case state machine of section 5.2, assignment and SLA")
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    /** Caps how much one request can pull, whatever size the caller asks for. */
    private static final int MAX_PAGE_SIZE = 100;

    private final OptimizationCaseService caseService;

    /**
     * Priority ordered by default; sort=sla puts whatever runs out first on top.
     * Admin is included because the role matrix grants them every record, and the detail
     * endpoint already did - being able to open a case but not list them made no sense.
     */
    @Operation(summary = "List optimization cases",
            description = """
                    Ordered by priority by default. sort=sla puts whatever runs out first on top,
                    which is the order a supervisor watches breaches in.

                    assignedTo accepts a user id or the literal me. An expert is always narrowed to
                    their own cases whatever they pass, since for them it is a rule rather than a
                    preference. size is clamped to 100 and a negative page becomes 0.

                    Roles: EXPERT, SUPERVISOR, ADMIN.""")
    @GetMapping
    public ApiResponse<PagedResult<CaseResponse>> list(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "priority") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            CallerIdentity caller) {

        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR, Role.ADMIN);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return ApiResponse.ok(caseService.list(status, resolveAssignedTo(assignedTo, caller),
                CaseSort.fromParam(sort), pageable));
    }

    @Operation(summary = "Get one case",
            description = """
                    Carries the AI segment, the conversion estimate, the recommendation score and
                    the remaining SLA in seconds, which is what the expert and supervisor screens
                    colour their rows by.

                    Roles: EXPERT, SUPERVISOR, ADMIN.""")
    @GetMapping("/{caseId}")
    public ApiResponse<CaseResponse> get(@PathVariable UUID caseId, CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR, Role.ADMIN);
        return ApiResponse.ok(caseService.getById(caseId, caller));
    }

    @Operation(summary = "Assign a case to an expert",
            description = """
                    The supervisor override of the automatic assignment. Not a transition of its
                    own, but a case still in YENI moves to ATANDI as a side effect, because an
                    assigned case is by definition no longer unassigned.

                    A case that is already closed cannot be assigned.

                    Roles: SUPERVISOR.""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Assigned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422",
                    description = "The case is already closed, error.code INVALID_STATE_TRANSITION",
                    content = @Content)
    })
    @PostMapping("/{caseId}/assign")
    public ApiResponse<CaseResponse> assign(@PathVariable UUID caseId,
                                            @Valid @RequestBody AssignRequest request,
                                            CallerIdentity caller) {
        caller.requireAnyOf(Role.SUPERVISOR);
        return ApiResponse.ok(caseService.assign(caseId, request));
    }

    @Operation(summary = "Move a case to another status",
            description = """
                    The state machine of section 5.2. Only these moves are allowed:

                    YENI to ATANDI, ATANDI to OPTIMIZE_EDILIYOR, OPTIMIZE_EDILIYOR to
                    TEST_EDILIYOR, TEST_EDILIYOR back to OPTIMIZE_EDILIYOR, OPTIMIZE_EDILIYOR to
                    TAMAMLANDI, TAMAMLANDI to YAYINDA, YAYINDA to ARSIVLENDI.

                    Anything else is refused with 422. Completing a case requires optimizationNote
                    and publishes campaign.optimized, which is what Gamification scores.

                    Roles: EXPERT, SUPERVISOR.""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Moved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Completing without a note, error.code OPTIMIZATION_NOTE_REQUIRED",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422",
                    description = "A move outside the table, error.code INVALID_STATE_TRANSITION",
                    content = @Content)
    })
    @PatchMapping("/{caseId}/status")
    public ApiResponse<CaseResponse> changeStatus(@PathVariable UUID caseId,
                                                  @Valid @RequestBody StatusChangeRequest request,
                                                  CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(caseService.changeStatus(caseId, request, caller));
    }

    /**
     * An expert only ever sees their own cases, whatever they asked for - the filter is
     * not a preference for them, it is the rule. Supervisors may filter by anyone.
     */
    private static UUID resolveAssignedTo(String assignedTo, CallerIdentity caller) {
        if (caller.isExpert()) {
            return caller.userId();
        }
        if (assignedTo == null || assignedTo.isBlank()) {
            return null;
        }
        if ("me".equals(assignedTo)) {
            return caller.userId();
        }
        try {
            return UUID.fromString(assignedTo);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "assignedTo must be 'me' or a user id");
        }
    }
}
