package com.offerhub.campaign.service;

import com.offerhub.campaign.entity.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * SLA windows from the case document, section 5.4. The numbers are fixed; only the unit
 * moves - SLA_TIME_UNIT=minutes turns the two hour KRITIK window into two minutes, so a
 * breach can be demonstrated without waiting two hours for it.
 */
@Slf4j
@Component
public class SlaPolicy {

    /** How many units each priority gets. KRITIK 2, YUKSEK 8, ORTA 24, DUSUK 72. */
    private static final Map<Priority, Integer> WINDOWS = Map.of(
            Priority.KRITIK, 2,
            Priority.YUKSEK, 8,
            Priority.ORTA, 24,
            Priority.DUSUK, 72);

    private final ChronoUnit unit;

    public SlaPolicy(@Value("${sla.time-unit:hours}") String timeUnit) {
        this.unit = parse(timeUnit);
        log.info("SLA windows are measured in {}", unit);
    }

    /**
     * The clock starts when the case is born, not when an expert picks it up - waiting
     * for an assignment must not buy extra time.
     */
    public Instant deadlineFor(Priority priority, Instant startedAt) {
        return startedAt.plus(WINDOWS.get(priority), unit);
    }

    /**
     * Static, because a countdown needs no configuration - only the deadline that was
     * already computed. Never stored: a saved countdown is stale the moment it is written,
     * so every read recomputes it.
     * Negative once the deadline has passed; null for cases created before this column
     * existed.
     */
    public static Long remainingSeconds(Instant deadline, Instant completedAt) {
        if (deadline == null) {
            return null;
        }
        // The clock stops on completion - a case finished in time stays in time forever.
        Instant reference = completedAt != null ? completedAt : Instant.now();
        return Duration.between(reference, deadline).toSeconds();
    }

    /** A typo in the environment must not silently change the demo, so it is logged. */
    private static ChronoUnit parse(String timeUnit) {
        try {
            return ChronoUnit.valueOf(timeUnit.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown SLA_TIME_UNIT '{}', falling back to hours", timeUnit);
            return ChronoUnit.HOURS;
        }
    }
}
