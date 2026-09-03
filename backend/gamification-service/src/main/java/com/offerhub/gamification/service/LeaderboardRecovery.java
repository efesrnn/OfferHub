package com.offerhub.gamification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Rebuilds the ranking on startup.
 *
 * Redis has no persistence configured, so a restarted container comes back empty while
 * Postgres still holds every scored entry. Without this the leaderboard would silently
 * disagree with the profile totals until the next event arrived - which is exactly the
 * drift we already had once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardRecovery implements ApplicationRunner {

    private final LeaderboardService leaderboardService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int experts = leaderboardService.rebuild();
            log.info("Leaderboard rebuilt from the ledger for {} expert entries", experts);
        } catch (Exception ex) {
            // A missing Redis must not stop the service: points still land in Postgres and
            // the ranking can be repaired later through the endpoint.
            log.error("Could not rebuild the leaderboard on startup", ex);
        }
    }
}
