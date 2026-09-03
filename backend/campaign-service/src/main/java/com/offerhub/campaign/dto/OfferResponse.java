package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.OfferStatus;
import com.offerhub.campaign.entity.SubscriberOffer;

import java.time.Instant;

public record OfferResponse(
        String offerId,
        String campaignNo,
        String title,
        String description,
        Integer discountRate,
        Instant validUntil,
        double score,
        boolean highlighted,
        String status,
        String type,
        Instant acceptedAt,
        Integer rating
) {
    /** conversionProbability henuz AI entegrasyonu kurulmadigi icin genelde null - notr 0.5 varsayilir. */
    public static OfferResponse from(SubscriberOffer offer) {
        Campaign campaign = offer.getCampaign();
        double score = campaign.getConversionProbability() != null
                ? campaign.getConversionProbability().doubleValue()
                : 0.5;

        return new OfferResponse(
                offer.getId().toString(),
                campaign.getCampaignNo(),
                campaign.getTitle(),
                null,
                campaign.getDiscountRate(),
                campaign.getValidUntil(),
                score,
                score > 0.80,
                offer.getStatus().name(),
                campaign.getType().name(),
                offer.getStatus() == OfferStatus.ACCEPTED ? offer.getRespondedAt() : null,
                offer.getRating()
        );
    }
}
