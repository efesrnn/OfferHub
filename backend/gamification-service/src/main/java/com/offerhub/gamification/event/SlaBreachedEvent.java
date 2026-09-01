package com.offerhub.gamification.event;

import java.time.Instant;
import java.util.UUID;

/** Our own copy of the sla.breached payload. */
public record SlaBreachedEvent(
        UUID caseId,
        String campaignNo,
        UUID expertId,
        String priority,
        Instant slaDeadline
) {
}
