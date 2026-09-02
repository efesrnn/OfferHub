package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.OptimizationCase;
import com.offerhub.campaign.entity.Priority;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload of sla.breached. expertId is null when nobody had picked the case up yet -
 * an unassigned breach is still a breach, it just costs no one points.
 */
public record SlaBreachedPayload(
        UUID caseId,
        String campaignNo,
        UUID expertId,
        Priority priority,
        Instant slaDeadline
) {

    public static SlaBreachedPayload from(OptimizationCase optimizationCase) {
        Campaign campaign = optimizationCase.getCampaign();
        return new SlaBreachedPayload(
                optimizationCase.getId(),
                campaign.getCampaignNo(),
                optimizationCase.getAssignedExpertId(),
                campaign.getPriority(),
                optimizationCase.getSlaDeadline());
    }
}
