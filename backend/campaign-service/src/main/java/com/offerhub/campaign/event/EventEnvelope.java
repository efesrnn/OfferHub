package com.offerhub.campaign.event;

import java.time.Instant;

/** Wire format - every message has this shape, only payload varies. */
public record EventEnvelope(String eventType, Instant timestamp, Object payload) {
}
