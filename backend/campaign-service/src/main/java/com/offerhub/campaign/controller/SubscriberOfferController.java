package com.offerhub.campaign.controller;

import com.offerhub.campaign.dto.ApiResponse;
import com.offerhub.campaign.dto.OfferActionResponse;
import com.offerhub.campaign.dto.RateOfferRequest;
import com.offerhub.campaign.dto.SubscriberOfferResponse;
import com.offerhub.campaign.entity.OfferStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The paths the mobile app was built against.
 *
 * These are a second door onto the same OfferService and the same offers table, not a
 * second implementation: one place decides who may answer an offer, when a rating is
 * final, and what an answer publishes. Two implementations would mean two rows per
 * subscriber and campaign, and a conversion rate counted twice.
 *
 * The split is only in the URL and the payload shape. /api/v1/offers follows the team's
 * contract; this follows the client's, where accept and decline are separate paths and a
 * rating is called "rating".
 */
@RestController
@RequestMapping("/api/v1/subscribers/me/offers")
@RequiredArgsConstructor
public class SubscriberOfferController {

    /** The client asks for a plain list, so the page size is ours to choose. */
    private static final int LIST_SIZE = 50;

    private final OfferService offerService;

    @GetMapping
    public ApiResponse<List<SubscriberOfferResponse>> list(CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(offerService.offersOf(caller.userId(), PageRequest.of(0, LIST_SIZE))
                .stream()
                .map(SubscriberOfferResponse::from)
                .toList());
    }

    @GetMapping("/{offerId}")
    public ApiResponse<SubscriberOfferResponse> get(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(SubscriberOfferResponse.from(offerService.offerOf(offerId, caller.userId())));
    }

    @PostMapping("/{offerId}/accept")
    public ApiResponse<OfferActionResponse> accept(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(OfferActionResponse.from(
                offerService.respondReturningOffer(offerId, OfferStatus.ACCEPTED, caller.userId())));
    }

    @PostMapping("/{offerId}/decline")
    public ApiResponse<OfferActionResponse> decline(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(OfferActionResponse.from(
                offerService.respondReturningOffer(offerId, OfferStatus.DECLINED, caller.userId())));
    }

    @PostMapping("/{offerId}/rating")
    public ApiResponse<OfferActionResponse> rate(@PathVariable UUID offerId,
                                                 @Valid @RequestBody RateOfferRequest request,
                                                 CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(OfferActionResponse.from(
                offerService.rateReturningOffer(offerId, request.rating(), caller.userId())));
    }
}
