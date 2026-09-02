package com.offerhub.gamification.event;

import java.util.UUID;

/**
 * Our own copy of the offer.rated payload. expertId is resolved by Campaign Service, which
 * owns the link from a campaign to the case and the expert who worked it - this service
 * has no campaign data to look it up with.
 */
public record OfferRatedEvent(
        UUID offerId,
        UUID subscriberId,
        String campaignNo,
        UUID expertId,
        Integer stars
) {
}
