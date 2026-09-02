package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.Offer;

import java.util.UUID;

/**
 * Payload of offer.rated. The case document requires a low rating to cost the expert
 * points (7.1, -3 for one or two stars), so the event has to name the expert who worked
 * the campaign - Gamification cannot look that up, it owns no campaign data.
 * expertId is null when no optimization case was ever opened, and then nobody is charged.
 */
public record OfferRatedPayload(
        UUID offerId,
        UUID subscriberId,
        String campaignNo,
        UUID expertId,
        Integer stars
) {

    public static OfferRatedPayload from(Offer offer, UUID expertId) {
        return new OfferRatedPayload(
                offer.getId(),
                offer.getSubscriberId(),
                offer.getCampaign().getCampaignNo(),
                expertId,
                offer.getStars());
    }
}
