package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.SubscriberOffer;

import java.util.UUID;

public record OfferRespondedPayload(
        UUID offerId,
        UUID subscriberId,
        String campaignNo,
        String response
) {
    public static OfferRespondedPayload from(SubscriberOffer offer) {
        return new OfferRespondedPayload(
                offer.getId(),
                offer.getSubscriberId(),
                offer.getCampaign().getCampaignNo(),
                offer.getStatus().name());
    }
}
