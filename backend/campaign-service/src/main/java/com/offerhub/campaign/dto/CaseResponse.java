package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.entity.OptimizationCase;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Case payload (docs/CAMPAIGN-API.md sections 6-9). Campaign fields are flattened in on
 * purpose: the expert's screen shows one card, not a case wrapping a campaign.
 */
public record CaseResponse(
        UUID caseId,
        String campaignNo,
        String title,
        Segment segment,
        Segment aiSegment,
        Priority priority,
        CaseStatus status,
        BigDecimal conversionProbability,
        BigDecimal recommendationScore,
        Instant slaDeadline,
        Long slaRemainingSeconds,
        String optimizationNote,
        UUID assignedExpertId,
        Instant createdAt,
        Instant completedAt
) {

    /** recommendationScore and the SLA pair stay null until the AI and SLA rounds. */
    public static CaseResponse from(OptimizationCase optimizationCase) {
        Campaign campaign = optimizationCase.getCampaign();
        return new CaseResponse(
                optimizationCase.getId(),
                campaign.getCampaignNo(),
                campaign.getTitle(),
                campaign.getSegment(),
                campaign.getAiSegment(),
                campaign.getPriority(),
                optimizationCase.getStatus(),
                campaign.getConversionProbability(),
                null,
                null,
                null,
                optimizationCase.getOptimizationNote(),
                optimizationCase.getAssignedExpertId(),
                optimizationCase.getCreatedAt(),
                optimizationCase.getCompletedAt());
    }
}
