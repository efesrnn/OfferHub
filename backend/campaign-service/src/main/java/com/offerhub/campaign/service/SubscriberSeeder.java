package com.offerhub.campaign.service;

import com.offerhub.campaign.entity.Segment;
import com.offerhub.campaign.entity.SubscriberProjection;
import com.offerhub.campaign.repository.SubscriberProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fills the subscriber projection from the same profile set AI Service trained on, so a
 * demo shows one consistent population instead of two unrelated ones.
 * Runs on startup and does nothing when the table already has rows - `docker compose up`
 * has to be repeatable, and a seeder that duplicates its data on every restart is worse
 * than no seeder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriberSeeder implements ApplicationRunner {

    private static final String CSV = "seed/subscriber_profiles.csv";
    private static final String BOM = "﻿";

    private final SubscriberProjectionRepository projectionRepository;

    @Value("${seed.enabled:true}")
    private boolean enabled;

    /**
     * The CSV identifies subscribers as SUB-0001; the system speaks UUID. Hashing the code
     * into a UUID keeps it deterministic - the same code always yields the same id, so
     * re-seeding is stable and any other service applying the same rule lands on the same
     * subscriber. A random id per run would break both.
     */
    public static UUID idOf(String externalRef) {
        return UUID.nameUUIDFromBytes(externalRef.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Subscriber seeding disabled");
            return;
        }
        if (projectionRepository.count() > 0) {
            log.info("Subscriber projection already populated, skipping seed");
            return;
        }

        List<SubscriberProjection> rows = read();
        projectionRepository.saveAll(rows);
        log.info("Seeded {} subscribers into the projection", rows.size());
    }

    private List<SubscriberProjection> read() {
        List<SubscriberProjection> rows = new ArrayList<>();
        Instant now = Instant.now();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(CSV).getInputStream(), StandardCharsets.UTF_8))) {

            // The file is exported with a byte order mark, which would otherwise end up
            // inside the first column name.
            String header = reader.readLine();
            if (header == null) {
                return rows;
            }
            List<String> columns = List.of(header.replace(BOM, "").trim().split(","));
            int refIndex = columns.indexOf("subscriberId");
            int segmentIndex = columns.indexOf("segment");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.trim().split(",");
                String externalRef = values[refIndex];

                rows.add(SubscriberProjection.builder()
                        .subscriberId(idOf(externalRef))
                        .externalRef(externalRef)
                        .segment(Segment.valueOf(values[segmentIndex]))
                        .syncedAt(now)
                        .build());
            }
        } catch (Exception ex) {
            // A broken fixture must not stop the service from serving traffic.
            log.error("Could not read {}, projection left empty", CSV, ex);
            return List.of();
        }
        return rows;
    }
}
