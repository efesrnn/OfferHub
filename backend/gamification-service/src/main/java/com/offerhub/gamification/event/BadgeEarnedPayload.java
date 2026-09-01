package com.offerhub.gamification.event;

import com.offerhub.gamification.entity.Badge;

import java.util.UUID;

/** Payload of badge.earned (EVENTS.md). */
public record BadgeEarnedPayload(UUID expertId, Badge badge, int totalPoints) {
}
