package com.offerhub.campaign.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The "Sistem" actor the case document keeps naming in section 5.2. Neither an SLA breach
 * nor an expiring campaign is triggered by a user, so something has to go looking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaseScheduler {

    private final OptimizationCaseService caseService;

    /**
     * fixedDelay, not fixedRate: the next scan starts after the previous one finished, so
     * a slow scan cannot pile up on top of itself. The interval is configurable because a
     * demo with SLA_TIME_UNIT=minutes needs to notice a breach in seconds.
     */
    @Scheduled(fixedDelayString = "${sla.scan-interval-ms:30000}")
    public void scanForBreaches() {
        int breached = caseService.markBreachedCases();
        if (breached > 0) {
            log.warn("{} case(s) breached their SLA", breached);
        }
    }

    /** Validity expiry is a matter of days, so this runs far less often than the SLA scan. */
    @Scheduled(fixedDelayString = "${campaign.archive-interval-ms:60000}")
    public void archiveExpiredCampaigns() {
        int archived = caseService.archiveExpiredCases();
        if (archived > 0) {
            log.info("{} case(s) archived after their campaign expired", archived);
        }
    }
}
