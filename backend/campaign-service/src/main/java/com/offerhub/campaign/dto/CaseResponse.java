package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CaseStatus;
import com.offerhub.campaign.entity.OptimizationCase;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.service.SlaPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


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

    /** recommendationScore stays null until AI Service is wired in. */
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
                optimizationCase.getSlaDeadline(),
                SlaPolicy.remainingSeconds(optimizationCase.getSlaDeadline(),
                        optimizationCase.getCompletedAt()),
                optimizationCase.getOptimizationNote(),
                optimizationCase.getAssignedExpertId(),
                optimizationCase.getCreatedAt(),
                optimizationCase.getCompletedAt());
    }
}
