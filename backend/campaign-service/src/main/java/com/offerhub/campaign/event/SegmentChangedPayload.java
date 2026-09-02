package com.offerhub.campaign.event;

import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.security.Role;

import java.util.UUID;


public record SegmentChangedPayload(
        String campaignNo,
        UUID changedBy,
        Role changedByRole,
        Segment originalSegment,
        Segment correctedSegment
) {
}
