package com.offerhub.campaign.client;

import java.math.BigDecimal;

/**
 * What AI Service answers for one subscriber and one campaign type.
 * Our own copy of its response shape - Campaign does not share classes with AI, so a
 * change on their side surfaces as a mapping problem here rather than a compile error
 * across two services.
 *
 * @param segment               AI's classification, one of the Segment names as text
 * @param conversionProbability 0.0-1.0, how likely this subscriber is to convert
 * @param score                 0.0-1.0, how well the campaign suits them
 */
public record AiRecommendation(
        String segment,
        BigDecimal conversionProbability,
        BigDecimal score
) {
}
