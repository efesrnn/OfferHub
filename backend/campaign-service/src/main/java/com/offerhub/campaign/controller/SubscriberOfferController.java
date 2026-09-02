package com.offerhub.campaign.controller;

import com.offerhub.campaign.dto.ApiResponse;
import com.offerhub.campaign.dto.OfferActionResponse;
import com.offerhub.campaign.dto.OfferResponse;
import com.offerhub.campaign.security.CallerIdentity;
import com.offerhub.campaign.security.Role;
import com.offerhub.campaign.service.SubscriberOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscribers/me/offers")
@RequiredArgsConstructor
public class SubscriberOfferController {

    private final SubscriberOfferService offerService;

    @GetMapping
    public ApiResponse<List<OfferResponse>> list(CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(offerService.listMyOffers(caller.userId()));
    }

    @GetMapping("/{offerId}")
    public ApiResponse<OfferResponse> get(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(offerService.getOne(caller.userId(), offerId));
    }

    @PostMapping("/{offerId}/accept")
    public ApiResponse<OfferActionResponse> accept(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(new OfferActionResponse(offerService.respond(caller.userId(), offerId, true)));
    }

    @PostMapping("/{offerId}/decline")
    public ApiResponse<OfferActionResponse> decline(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(new OfferActionResponse(offerService.respond(caller.userId(), offerId, false)));
    }
}
