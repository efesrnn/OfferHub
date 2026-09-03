package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Offer;

/** Wrapper the mobile client expects back from accept, decline and rating. */
public record OfferActionResponse(SubscriberOfferResponse offer) {

    public static OfferActionResponse from(Offer offer) {
        return new OfferActionResponse(SubscriberOfferResponse.from(offer));
    }
}
