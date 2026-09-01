package com.offerhub.gamification.dto;

import java.util.UUID;

/**
 * @param name null for now - expert names belong to Identity Service and this service
 *             does not call it; the client resolves the id, or a projection fills it later
 */
public record LeaderboardEntry(int rank, UUID expertId, String name, int points) {
}
