package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.Offer;
import com.offerhub.campaign.entity.OfferStatus;

import java.util.UUID;

/**
 * Payload of offer.responded. AI Service uses it to move the recommendation score:
 * a declined offer should lower the score of campaigns like it.
 */
public record OfferRespondedPayload(
        UUID offerId,
        UUID subscriberId,
        String campaignNo,
        OfferStatus response
) {

    public static OfferRespondedPayload from(Offer offer) {
        return new OfferRespondedPayload(
                offer.getId(),
                offer.getSubscriberId(),
                offer.getCampaign().getCampaignNo(),
                offer.getStatus());
    }
}
