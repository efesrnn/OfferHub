package com.offerhub.gamification.dto;

import com.offerhub.gamification.entity.Badge;

import java.time.Instant;

/** Every badge in the catalog, earned or not - the client can show locked ones greyed out. */
public record BadgeResponse(Badge badge, boolean earned, Instant earnedAt) {
}
