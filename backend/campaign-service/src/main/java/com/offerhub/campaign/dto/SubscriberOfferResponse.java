package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.Offer;
import com.offerhub.campaign.entity.OfferStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The offer as the mobile app expects it (SubscriberApi / OfferDto).
 *
 * Deliberately separate from OfferResponse: this shape is owned by the client, ours is
 * owned by the contract in docs/CAMPAIGN-API.md. Keeping them apart means either can
 * change without dragging the other along - the two are views of one Offer row, not two
 * implementations of an offer.
 *
 * @param score              not nullable on the client, so an unscored offer reports 0.0. The
 *                           model is a sigmoid and never actually returns zero, which makes 0.0
 *                           readable as "not scored" rather than "scored badly".
 * @param previousAcceptedCount how many subscribers, across everybody this campaign was ever
 *                           offered to, accepted it - the social proof shown on the detail
 *                           screen. 0 on the plain list mapping, where computing it for every
 *                           row would cost one query per offer; only the detail endpoint fills
 *                           it in.
 * @param averageRating      mean of 1-5 stars among those who rated it, null when nobody has -
 *                           null rather than 0 so the client can tell "no ratings" from "rated
 *                           badly". Same list/detail split as previousAcceptedCount.
 * @param ratingCount        how many ratings the average is built from, the denominator a
 *                           lone number can't otherwise be judged against.
 */
public record SubscriberOfferResponse(
        String offerId,
        String campaignNo,
        String title,
        String description,
        Integer discountRate,
        Instant validUntil,
        BigDecimal score,
        boolean highlighted,
        String status,
        String type,
        Instant acceptedAt,
        Integer rating,
        long previousAcceptedCount,
        BigDecimal averageRating,
        long ratingCount
) {

    /** Case document 6.1: a score above 0.80 is shown first and marked. */
    private static final BigDecimal HIGHLIGHT_THRESHOLD = new BigDecimal("0.80");

    /** The list mapping - no social proof, so no extra query per row. */
    public static SubscriberOfferResponse from(Offer offer) {
        return from(offer, 0, null, 0);
    }

    /** The detail mapping - the caller already paid for the social-proof queries. */
    public static SubscriberOfferResponse from(Offer offer, long previousAcceptedCount,
                                               BigDecimal averageRating, long ratingCount) {
        Campaign campaign = offer.getCampaign();
        BigDecimal score = offer.getScore();

        return new SubscriberOfferResponse(
                offer.getId().toString(),
                campaign.getCampaignNo(),
                campaign.getTitle(),
                // Campaigns carry no free text description; the field stays for the client.
                null,
                campaign.getDiscountRate(),
                campaign.getValidUntil(),
                score == null ? BigDecimal.ZERO : score,
                score != null && score.compareTo(HIGHLIGHT_THRESHOLD) > 0,
                offer.getStatus().name(),
                campaign.getType().name(),
                offer.getStatus() == OfferStatus.ACCEPTED ? offer.getRespondedAt() : null,
                offer.getStars(),
                previousAcceptedCount,
                averageRating,
                ratingCount);
    }
}
