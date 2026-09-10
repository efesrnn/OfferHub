package com.offerhub.campaign.controller;

import com.offerhub.campaign.dto.ApiResponse;
import com.offerhub.campaign.dto.OfferActionResponse;
import com.offerhub.campaign.dto.RateOfferRequest;
import com.offerhub.campaign.dto.SubscriberOfferResponse;
import com.offerhub.campaign.entity.OfferStatus;
import com.offerhub.campaign.security.CallerIdentity;
import com.offerhub.campaign.security.Role;
import com.offerhub.campaign.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Subscriber offers", description = "The same offers under the paths the mobile client calls")
@RequestMapping("/api/v1/subscribers/me/offers")
@RequiredArgsConstructor
public class SubscriberOfferController {

    /** The client asks for a plain list, so the page size is ours to choose. */
    private static final int LIST_SIZE = 50;

    private final OfferService offerService;

    @Operation(summary = "List the caller's offers",
            description = """
                    The same rules as GET /api/v1/offers, returned as a plain array because that is
                    what the client expects. Scored below 0.60 is not shown, above 0.80 is
                    highlighted, and the list arrives score ordered.

                    Roles: SUBSCRIBER.""")
    @GetMapping
    public ApiResponse<List<SubscriberOfferResponse>> list(CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(offerService.offersOf(caller.userId(), PageRequest.of(0, LIST_SIZE))
                .stream()
                .map(SubscriberOfferResponse::from)
                .toList());
    }

    @Operation(summary = "Get one offer",
            description = """
                    Reading someone else's offer id returns 403 rather than 404: ownership is
                    checked against the caller in the token, so changing the id in the path gets
                    nothing.

                    Roles: SUBSCRIBER.""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "The offer"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "The offer belongs to another subscriber",
                    content = @Content)
    })
    @GetMapping("/{offerId}")
    public ApiResponse<SubscriberOfferResponse> get(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(SubscriberOfferResponse.from(offerService.offerOf(offerId, caller.userId())));
    }

    @Operation(summary = "Accept an offer",
            description = """
                    Records the conversion and publishes offer.responded. An offer can only be
                    answered once.

                    Roles: SUBSCRIBER.""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Already answered, error.code OFFER_ALREADY_RESPONDED",
                    content = @Content)
    })
    @PostMapping("/{offerId}/accept")
    public ApiResponse<OfferActionResponse> accept(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(OfferActionResponse.from(
                offerService.respondReturningOffer(offerId, OfferStatus.ACCEPTED, caller.userId())));
    }

    @Operation(summary = "Decline an offer",
            description = """
                    Section 5.5: a decline lowers the recommendation score of similar campaigns,
                    which is what offer.responded carries to AI.

                    Roles: SUBSCRIBER.""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Declined"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Already answered, error.code OFFER_ALREADY_RESPONDED",
                    content = @Content)
    })
    @PostMapping("/{offerId}/decline")
    public ApiResponse<OfferActionResponse> decline(@PathVariable UUID offerId, CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(OfferActionResponse.from(
                offerService.respondReturningOffer(offerId, OfferStatus.DECLINED, caller.userId())));
    }

    @Operation(summary = "Rate the experience from 1 to 5",
            description = """
                    Section 5.6: rating is once only. A rating of 1 or 2 publishes offer.rated and
                    costs the expert 3 points, which is how an irrelevant offer reaches the person
                    who targeted it.

                    Roles: SUBSCRIBER.""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Rating recorded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Outside 1 to 5, error.code VALIDATION_ERROR",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Already rated, error.code OFFER_ALREADY_RATED",
                    content = @Content)
    })
    @PostMapping("/{offerId}/rating")
    public ApiResponse<OfferActionResponse> rate(@PathVariable UUID offerId,
                                                 @Valid @RequestBody RateOfferRequest request,
                                                 CallerIdentity caller) {
        caller.requireAnyOf(Role.SUBSCRIBER);
        return ApiResponse.ok(OfferActionResponse.from(
                offerService.rateReturningOffer(offerId, request.rating(), caller.userId())));
    }
}
