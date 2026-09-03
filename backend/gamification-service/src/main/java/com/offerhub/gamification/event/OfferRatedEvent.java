package com.offerhub.gamification.event;

import java.util.UUID;

/** Our own copy of the offer.rated payload. */
public record OfferRatedEvent(
        UUID offerId,
        UUID subscriberId,
        String campaignNo,
        UUID expertId,
        Integer rating
) {
}
