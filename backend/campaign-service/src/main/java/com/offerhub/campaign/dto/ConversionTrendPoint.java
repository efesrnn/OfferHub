package com.offerhub.campaign.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One day of the conversion trend line.
 *
 * @param answered offers the subscriber actually responded to that day - the denominator,
 *                 sent along so the client can tell a real 100% from a single accepted offer
 */
public record ConversionTrendPoint(
        LocalDate date,
        BigDecimal conversionRate,
        long answered
) {
}
