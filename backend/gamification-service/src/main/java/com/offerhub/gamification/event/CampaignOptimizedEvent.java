package com.offerhub.gamification.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Our own copy of the campaign.optimized payload. Deliberately not shared with Campaign
 * Service: a shared class would couple the two services' release cycles together.
 * Fields we do not use are simply absent - unknown JSON properties are ignored.
 */
public record CampaignOptimizedEvent(
        UUID caseId,
        String campaignNo,
        UUID expertId,
        String segment,
        String priority,
        BigDecimal conversionLift,
        Instant createdAt,
        Instant completedAt,
        Instant slaDeadline
) {
}
