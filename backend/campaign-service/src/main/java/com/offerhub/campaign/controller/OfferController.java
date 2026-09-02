package com.offerhub.campaign.controller;

import com.offerhub.campaign.dto.ApiResponse;
import com.offerhub.campaign.dto.OfferRateRequest;
import com.offerhub.campaign.dto.OfferRespondRequest;
import com.offerhub.campaign.dto.OfferResponse;
import com.offerhub.campaign.dto.PagedResult;
import com.offerhub.campaign.security.CallerIdentity;
import com.offerhub.campaign.security.Role;
import com.offerhub.campaign.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The subscriber side of the service. No subscriber id appears in any path or body - it
 * always comes from the token, so there is nothing for a caller to swap.
 */
@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class OfferController {

    /** Caps how much one request can pull, whatever size the caller asks for. */
    private static final int MAX_PAGE_SIZE = 100;

    private final OfferService offerService;

    @GetMapping
    public ApiResponse<PagedResult<OfferResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            CallerIdentity caller) {

        caller.requireAnyOf(Role.SUBSCRIBER);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return ApiResponse.ok(offerService.listFor(caller.userId(), pageable));
    }

    @PostMapping("/{offerId}/respond")
    public ApiResponse<OfferResponse> respond(@PathVariable UUID offerId,
                                              @Valid @RequestBody OfferRespondRequest request,
                                              CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(offerService.respond(offerId, request, caller.userId()));
    }

    @PostMapping("/{offerId}/rate")
    public ApiResponse<OfferResponse> rate(@PathVariable UUID offerId,
                                           @Valid @RequestBody OfferRateRequest request,
                                           CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(offerService.rate(offerId, request, caller.userId()));
    }
}
