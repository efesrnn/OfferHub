package com.offerhub.gamification.service;

import com.offerhub.gamification.entity.ExpertProfile;
import com.offerhub.gamification.entity.PointEntry;
import com.offerhub.gamification.entity.PointReason;
import com.offerhub.gamification.event.CampaignOptimizedEvent;
import com.offerhub.gamification.event.SlaBreachedEvent;
import com.offerhub.gamification.repository.ExpertProfileRepository;
import com.offerhub.gamification.repository.PointEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

/** Turns an event into point entries, following the scoring table in the case document. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    /** Case document 7.1: the bonus is for finishing in under two hours. */
    private static final Duration FAST_OPTIMIZATION_LIMIT = Duration.ofHours(2);

    /** KRITIK cases have a two hour SLA, so the same duration decides both bonuses. */
    private static final Duration CRITICAL_SLA = Duration.ofHours(2);

    /** "Conversion target exceeded" needs a target; ten points of lift is ours. */
    private static final BigDecimal CONVERSION_TARGET = new BigDecimal("0.10");

    private static final String CRITICAL_PRIORITY = "KRITIK";

    private final PointEntryRepository pointEntryRepository;
    private final ExpertProfileRepository profileRepository;
    private final BadgeService badgeService;
    private final LeaderboardService leaderboardService;

    @Transactional
    public void score(CampaignOptimizedEvent event) {
        if (event.expertId() == null) {
            log.warn("case {} completed with no expert assigned, nothing to score", event.caseId());
            return;
        }

        Duration took = Duration.between(event.createdAt(), event.completedAt());

        // Only a first delivery counts as a resolved case; a redelivery must not inflate it.
        if (award(event.expertId(), event.caseId(), PointReason.OPTIMIZATION_COMPLETED, event.segment())) {
            ExpertProfile expertProfile = profile(event.expertId());
            expertProfile.setCasesResolved(expertProfile.getCasesResolved() + 1);
        }

        if (took.compareTo(FAST_OPTIMIZATION_LIMIT) < 0) {
            award(event.expertId(), event.caseId(), PointReason.FAST_OPTIMIZATION, event.segment());
        }
        if (event.conversionLift() != null && event.conversionLift().compareTo(CONVERSION_TARGET) > 0) {
            award(event.expertId(), event.caseId(), PointReason.CONVERSION_TARGET_EXCEEDED, event.segment());
        }
        if (CRITICAL_PRIORITY.equals(event.priority()) && took.compareTo(CRITICAL_SLA) < 0) {
            award(event.expertId(), event.caseId(), PointReason.CRITICAL_WITHIN_SLA, event.segment());
        }

        // Always re-evaluated: badges are guarded by their own unique constraint, so a
        // repeat run is harmless and heals a badge missed by an earlier failure.
        badgeService.evaluate(event.expertId());
    }

    @Transactional
    public void score(SlaBreachedEvent event) {
        if (event.expertId() == null) {
            return;
        }
        award(event.expertId(), event.caseId(), PointReason.SLA_BREACH, null);
    }

    /**
     * Skips silently when this exact (source, reason) was already scored. RabbitMQ
     * delivers at least once, so the same event can arrive twice - without this check a
     * redelivery would pay the expert a second time.
     */
    private boolean award(UUID expertId, UUID sourceId, PointReason reason, String segment) {
        if (pointEntryRepository.existsBySourceIdAndReason(sourceId, reason)) {
            log.info("Already scored {} for {}, skipping", reason, sourceId);
            return false;
        }

        pointEntryRepository.save(PointEntry.builder()
                .expertId(expertId)
                .sourceId(sourceId)
                .reason(reason)
                .points(reason.getPoints())
                .segment(segment)
                .build());

        ExpertProfile expertProfile = profile(expertId);
        expertProfile.setTotalPoints(expertProfile.getTotalPoints() + reason.getPoints());

        // Redis holds the ranking only; the entry above is the record that can rebuild it.
        leaderboardService.addPoints(expertId, reason.getPoints());

        log.info("Expert {} {} {} points for {}", expertId,
                reason.getPoints() >= 0 ? "earned" : "lost", Math.abs(reason.getPoints()), reason);
        return true;
    }

    /** First event for an expert creates their profile - no registration step needed. */
    private ExpertProfile profile(UUID expertId) {
        return profileRepository.findById(expertId)
                .orElseGet(() -> profileRepository.save(ExpertProfile.empty(expertId)));
    }
}
