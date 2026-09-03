package com.offerhub.campaign.service;

import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;

import java.math.BigDecimal;

/**
 * What AI concluded about a campaign as a whole, plus the priority that follows from it.
 *
 * @param segment               AI's classification, BELIRSIZ when it could not be reached
 * @param conversionProbability null when unknown - which is what puts a campaign in the
 *                              manual optimization queue
 * @param recommendationScore   how well the campaign suits the segment it targets
 */
public record CampaignScoring(
        Segment segment,
        BigDecimal conversionProbability,
        BigDecimal recommendationScore,
        Priority priority
) {

    /**
     * The contract's fallback (case document 5.1): AI unreachable means BELIRSIZ and ORTA,
     * with no probability at all. A guessed number here would be worse than none - a null
     * probability is what tells the rest of the service that a human has to look at this.
     */
    public static CampaignScoring unavailable() {
        return new CampaignScoring(Segment.BELIRSIZ, null, null, Priority.ORTA);
    }
}
