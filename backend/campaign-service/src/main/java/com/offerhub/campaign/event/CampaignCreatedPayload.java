package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.Campaign;
import com.offerhub.campaign.entity.CampaignType;
import com.offerhub.campaign.entity.Priority;
import com.offerhub.campaign.entity.Segment;

import java.util.UUID;

/** Payload of campaign.created. */
public record CampaignCreatedPayload(
        String campaignNo,
        CampaignType type,
        Segment targetSegment,
        Priority priority,
        UUID createdBy
) {

    public static CampaignCreatedPayload from(Campaign campaign) {
        return new CampaignCreatedPayload(
                campaign.getCampaignNo(),
                campaign.getType(),
                campaign.getTargetSegment(),
                campaign.getPriority(),
                campaign.getCreatedBy());
    }
}
