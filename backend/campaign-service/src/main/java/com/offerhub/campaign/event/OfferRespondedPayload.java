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
        String subscriberRef,
        String campaignNo,
        OfferStatus response
) {

    /**
     * Carries both identifiers on purpose. subscriberId is the UUID our services agreed on;
     * subscriberRef is the readable code AI keys its profiles by, and without it AI cannot
     * tell which profile just declined an offer. Null when this subscriber did not come
     * from the seed set, and AI then has nothing to update - which is the honest outcome.
     */
    public static OfferRespondedPayload from(Offer offer, String subscriberRef) {
        return new OfferRespondedPayload(
                offer.getId(),
                offer.getSubscriberId(),
                subscriberRef,
                offer.getCampaign().getCampaignNo(),
                offer.getStatus());
    }
}
