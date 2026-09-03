package com.offerhub.campaign.dto;

import com.offerhub.campaign.entity.Segment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @param segmentDistribution    campaign count per segment, every segment present even at zero
 * @param conversionRate         accepted offers over answered offers, 0.00-1.00
 * @param slaComplianceRate      cases that never breached, over cases that have a deadline
 * @param slaBreachedActiveCases breached and still unfinished - the red card
 * @param pendingQueueCount      cases nobody has been assigned to yet
 * @param aiAccuracyRate         classifications nobody corrected, over classifications made
 * @param aiClassifiedCampaigns  how many classifications that rate is based on. Sent with
 *                               the rate on purpose: a campaign nobody has reviewed counts
 *                               as correct, so the rate starts at 1.00 and can only fall.
 *                               Without the denominator "100% accurate" reads as a result
 *                               rather than as "four campaigns, none corrected yet".
 * @param conversionTrend        the last two weeks, oldest first
 * @param expertPerformance      one row per expert who has finished something
 */
public record DashboardResponse(
        Map<Segment, Long> segmentDistribution,
        BigDecimal conversionRate,
        BigDecimal slaComplianceRate,
        long slaBreachedActiveCases,
        long pendingQueueCount,
        BigDecimal aiAccuracyRate,
        long aiClassifiedCampaigns,
        List<ConversionTrendPoint> conversionTrend,
        List<ExpertPerformance> expertPerformance
) {
}
