package com.offerhub.campaign.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @param expertId a staff user owned by Identity Service - Campaign cannot verify it
 *                 exists, so the id is taken at face value
 */
public record AssignRequest(@NotNull UUID expertId) {
}
