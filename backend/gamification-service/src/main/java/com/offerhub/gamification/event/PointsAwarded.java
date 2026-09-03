package com.offerhub.gamification.event;

import java.util.UUID;

/**
 * Raised inside the scoring transaction, applied to Redis only after it commits.
 * An in-process signal, not something that goes on the wire.
 */
public record PointsAwarded(UUID expertId, int points) {
}
