package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.SubscriberOffer;

import java.util.UUID;

public record OfferRatedPayload(
        UUID offerId,
        UUID subscriberId,
        String campaignNo,
        int rating
) {
    public static OfferRatedPayload from(SubscriberOffer offer) {
        return new OfferRatedPayload(
                offer.getId(),
                offer.getSubscriberId(),
                offer.getCampaign().getCampaignNo(),
                offer.getRating());
    }
}
