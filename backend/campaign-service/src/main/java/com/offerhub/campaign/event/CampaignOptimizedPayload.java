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
        Instant completedAt,
        Instant slaDeadline
) {

    /**
     * slaDeadline travels with the event on purpose: Campaign owns SLA_TIME_UNIT, so it
     * is the only service that can say when this case was due. Gamification comparing
     * completedAt against a two hour constant of its own would pay the "KRITIK within
     * SLA" bonus to a breached case every time a demo shortens the unit.
     * conversionLift stays null until AI Service can score a campaign before and after.
     */
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
                optimizationCase.getCompletedAt(),
                optimizationCase.getSlaDeadline());
    }
}
