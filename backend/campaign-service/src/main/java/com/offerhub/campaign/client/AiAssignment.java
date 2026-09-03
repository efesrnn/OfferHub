package com.offerhub.campaign.client;

import java.math.BigDecimal;

/**
 * AI's answer to "who should work this case".
 *
 * @param expertId   the expert's own identifier as AI knows it, null when nobody is free
 * @param matchScore the assignment score behind the choice, kept for the audit trail
 * @param queued     true when every expert is at capacity, so the case waits
 */
public record AiAssignment(
        String expertId,
        BigDecimal matchScore,
        boolean queued
) {
}
