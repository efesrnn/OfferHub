package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.SubscriberOffer;

import java.util.UUID;

public record OfferRatedPayload(
        UUID offerId,
        UUID subscriberId,
        String campaignNo,
        UUID expertId,
        int rating
) {
    /** expertId = kampanyayi olusturan kisi (createdBy). Optimizasyonu yapan uzmanla
     * farkli olabilir - basitlik icin puan cezasi kampanyanin sahibine yazilir. */
    public static OfferRatedPayload from(SubscriberOffer offer) {
        return new OfferRatedPayload(
                offer.getId(),
                offer.getSubscriberId(),
                offer.getCampaign().getCampaignNo(),
                offer.getCampaign().getCreatedBy(),
                offer.getRating());
    }
}
