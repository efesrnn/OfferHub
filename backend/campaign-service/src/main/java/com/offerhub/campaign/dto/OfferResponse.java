package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignType;
import com.offerhub.campaign.entity.Offer;
import com.offerhub.campaign.entity.OfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Offer payload */
public record OfferResponse(
        UUID offerId,
        String campaignNo,
        String title,
        CampaignType type,
        Integer discountRate,
        Instant validUntil,
        BigDecimal score,
        boolean highlighted,
        OfferStatus status,
        Integer stars
) {

    private static final BigDecimal HIGHLIGHT_THRESHOLD = new BigDecimal("0.80");

    public static OfferResponse from(Offer offer) {
        Campaign campaign = offer.getCampaign();
        BigDecimal score = offer.getScore();

        return new OfferResponse(
                offer.getId(),
                campaign.getCampaignNo(),
                campaign.getTitle(),
                campaign.getType(),
                campaign.getDiscountRate(),
                campaign.getValidUntil(),
                score,
                score != null && score.compareTo(HIGHLIGHT_THRESHOLD) > 0,
                offer.getStatus(),
                offer.getStars());
    }
}
