package com.offerhub.campaign.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Bridges the readable codes other services use (SUB-0001, EXP-001) to the UUIDs this
 * service stores.
 *
 * The team agreed on UUID identifiers (ORTAK-KARARLAR C2), but AI Service keys its
 * training data by readable code. Hashing the code into a UUID is deterministic - the
 * same code always yields the same id - so re-seeding is stable and any service applying
 * the same rule lands on the same subject.
 *
 * This is a bridge, not a solution: an expert who signs in through Identity has a
 * randomly generated UUID that no code hashes to, so an id derived here does not point at
 * a real account. Until the three services agree on one identifier, an automatically
 * assigned expert is a placeholder a supervisor is expected to correct.
 */
public final class ExternalIds {

    private ExternalIds() {
    }

    /** Already a UUID? Keep it. Otherwise derive one from the code. */
    public static UUID toUuid(String reference) {
        try {
            return UUID.fromString(reference);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(reference.getBytes(StandardCharsets.UTF_8));
        }
    }
}
