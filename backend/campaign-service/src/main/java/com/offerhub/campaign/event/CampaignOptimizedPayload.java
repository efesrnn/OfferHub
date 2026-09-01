package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.OptimizationCase;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload of campaign.optimized . Gamification turns this into points, so it
 * carries everything the scoring table needs: who, how urgent, how long it took.
 */
public record CampaignOptimizedPayload(
        UUID caseId,
        String campaignNo,
        UUID expertId,
        Segment segment,
        Priority priority,
        BigDecimal conversionLift,
        Instant createdAt,
        Instant completedAt
) {

    /** conversionLift stays null until AI Service can score a campaign before and after. */
    public static CampaignOptimizedPayload from(OptimizationCase optimizationCase) {
        Campaign campaign = optimizationCase.getCampaign();
        return new CampaignOptimizedPayload(
                optimizationCase.getId(),
                campaign.getCampaignNo(),
                optimizationCase.getAssignedExpertId(),
                campaign.getSegment(),
                campaign.getPriority(),
                null,
                optimizationCase.getCreatedAt(),
                optimizationCase.getCompletedAt());
    }
}
