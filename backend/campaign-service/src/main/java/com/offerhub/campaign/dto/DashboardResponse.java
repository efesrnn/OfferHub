package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Segment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @param segmentDistribution    campaign count per segment, every segment present even at zero
 * @param conversionRate         accepted offers over answered offers, 0.00-1.00
 * @param slaComplianceRate      cases that never breached, over cases that have a deadline
 * @param slaBreachedActiveCases breached and still unfinished - the red card
 * @param pendingQueueCount      cases nobody has been assigned to yet
 */
public record DashboardResponse(
        Map<Segment, Long> segmentDistribution,
        BigDecimal conversionRate,
        BigDecimal slaComplianceRate,
        long slaBreachedActiveCases,
        long pendingQueueCount
) {
}
