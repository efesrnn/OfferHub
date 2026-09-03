package com.offerhub.campaign.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Translates the expert codes AI answers with (EXP-001) into the staff accounts Identity
 * issues.
 *
 * The three services do not share an identifier for an expert: AI keys its capacity and
 * performance snapshots by its own code, Identity generates a UUID per staff account, and
 * this service stores that UUID on the case. Deriving a UUID from the code is stable but
 * points at nobody - an automatically assigned case could then not be opened by any real
 * expert, because the ownership check would correctly say it belongs to someone else.
 *
 * So the mapping is configuration. Fill in AI_EXPERT_MAPPING with the real staff ids and
 * automatic assignment lands on real accounts; leave it empty and it falls back to the
 * derived id, which is fine for testing and visible in the logs as such.
 */
@Slf4j
@Component
public class ExpertDirectory {

    private final Map<String, UUID> byCode = new HashMap<>();

    public ExpertDirectory(@Value("${ai.expert-mapping:}") String mapping) {
        parse(mapping);
        log.info("Expert directory holds {} mapped code(s)", byCode.size());
    }

    /**
     * @return the mapped staff account, or an id derived from the code when the mapping
     *         has no entry for it
     */
    public UUID resolve(String expertCode) {
        UUID mapped = byCode.get(expertCode);
        if (mapped != null) {
            return mapped;
        }

        log.warn("No staff account mapped for {}, assigning a placeholder id", expertCode);
        return ExternalIds.toUuid(expertCode);
    }

    public boolean isMapped(String expertCode) {
        return byCode.containsKey(expertCode);
    }

    /** Format: EXP-001=uuid,EXP-002=uuid. A malformed pair is skipped, not fatal. */
    private void parse(String mapping) {
        if (mapping == null || mapping.isBlank()) {
            return;
        }
        for (String pair : mapping.split(",")) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length != 2) {
                log.warn("Ignoring malformed expert mapping entry: {}", pair);
                continue;
            }
            try {
                byCode.put(parts[0].trim(), UUID.fromString(parts[1].trim()));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring expert mapping with a non UUID value: {}", pair);
            }
        }
    }
}
