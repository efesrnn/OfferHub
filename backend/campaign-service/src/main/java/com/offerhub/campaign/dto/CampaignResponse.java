package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignStatus;
import com.offerhub.campaign.entity.CampaignType;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;

import java.math.BigDecimal;
import java.time.Instant;

public record CampaignResponse(
        String campaignNo,
        String title,
        CampaignType type,
        Segment targetSegment,
        Segment aiSegment,
        Segment segment,
        Integer discountRate,
        Instant validUntil,
        CampaignStatus status,
        Priority priority,
        BigDecimal conversionProbability,
        BigDecimal recommendationScore,
        Instant createdAt
) {

    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getCampaignNo(),
                campaign.getTitle(),
                campaign.getType(),
                campaign.getTargetSegment(),
                campaign.getAiSegment(),
                campaign.getSegment(),
                campaign.getDiscountRate(),
                campaign.getValidUntil(),
                campaign.getStatus(),
                campaign.getPriority(),
                campaign.getConversionProbability(),
                campaign.getRecommendationScore(),
                campaign.getCreatedAt());
    }
}