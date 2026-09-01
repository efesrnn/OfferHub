package com.offerhub.gamification.event;

import java.time.Instant;

/** Wire format from EVENTS.md - every message has this shape, only payload varies. */
public record EventEnvelope(String eventType, Instant timestamp, Object payload) {
}
