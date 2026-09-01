package com.offerhub.gamification.service;

import com.offerhub.gamification.config.RabbitConfig;
import com.offerhub.gamification.entity.Badge;
import com.offerhub.gamification.entity.EarnedBadge;
import com.offerhub.gamification.entity.ExpertProfile;
import com.offerhub.gamification.entity.PointReason;
import com.offerhub.gamification.event.BadgeEarnedPayload;
import com.offerhub.gamification.event.EventEnvelope;
import com.offerhub.gamification.repository.EarnedBadgeRepository;
import com.offerhub.gamification.repository.ExpertProfileRepository;
import com.offerhub.gamification.repository.PointEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Badge conditions from the case document, section 7.2. Every rule is a count over
 * point_entries, so a badge can always be justified from the ledger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private static final String BADGE_EARNED = "badge.earned";
    private static final String CHURN_SEGMENT = "RISKLI_KAYIP";

    /** "In one day" read as a moving 24 hour window - no timezone argument to lose. */
    private static final Duration MARATHON_WINDOW = Duration.ofHours(24);

    private final PointEntryRepository pointEntryRepository;
    private final EarnedBadgeRepository badgeRepository;
    private final ExpertProfileRepository profileRepository;
    private final RabbitTemplate rabbitTemplate;

    /** Called after every scored event; awards whatever the expert now qualifies for. */
    @Transactional
    public void evaluate(UUID expertId) {
        long completed = pointEntryRepository.countByExpertIdAndReason(
                expertId, PointReason.OPTIMIZATION_COMPLETED);

        awardIf(expertId, Badge.ILK_KAMPANYA, completed >= 1);

        awardIf(expertId, Badge.HIZ_USTASI, pointEntryRepository.countByExpertIdAndReason(
                expertId, PointReason.FAST_OPTIMIZATION) >= 10);

        awardIf(expertId, Badge.DONUSUM_KRALI, pointEntryRepository.countByExpertIdAndReason(
                expertId, PointReason.CONVERSION_TARGET_EXCEEDED) >= 10);

        awardIf(expertId, Badge.MARATONCU, pointEntryRepository.countByExpertIdAndReasonAndEarnedAtAfter(
                expertId, PointReason.OPTIMIZATION_COMPLETED, Instant.now().minus(MARATHON_WINDOW)) >= 20);

        awardIf(expertId, Badge.CHURN_AVCISI, pointEntryRepository.countByExpertIdAndReasonAndSegment(
                expertId, PointReason.OPTIMIZATION_COMPLETED, CHURN_SEGMENT) >= 10);

        awardIf(expertId, Badge.UZMAN, largestSegmentCount(expertId) >= 50);
    }

    private long largestSegmentCount(UUID expertId) {
        List<Long> counts = pointEntryRepository.countsPerSegment(
                expertId, PointReason.OPTIMIZATION_COMPLETED);
        return counts.stream().max(Comparator.naturalOrder()).orElse(0L);
    }

    private void awardIf(UUID expertId, Badge badge, boolean earned) {
        if (!earned || badgeRepository.existsByExpertIdAndBadge(expertId, badge)) {
            return;
        }

        badgeRepository.save(EarnedBadge.builder().expertId(expertId).badge(badge).build());
        log.info("Expert {} earned badge {}", expertId, badge);

        int totalPoints = profileRepository.findById(expertId)
                .map(ExpertProfile::getTotalPoints)
                .orElse(0);

        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, BADGE_EARNED,
                new EventEnvelope(BADGE_EARNED, Instant.now(),
                        new BadgeEarnedPayload(expertId, badge, totalPoints)));
    }
}
