package com.offerhub.campaign.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One expert's row on the supervisor's performance card, case document 8.1.
 *
 * @param completedCases          optimizations finished
 * @param averageConversionLift   mean measured improvement, null while nothing was measured
 * @param averageCompletionHours  mean time from case opened to completed
 * @param activeCaseCount         what they are carrying now, against a capacity of ten
 */
public record ExpertPerformance(
        UUID expertId,
        long completedCases,
        BigDecimal averageConversionLift,
        BigDecimal averageCompletionHours,
        long activeCaseCount
) {
}
