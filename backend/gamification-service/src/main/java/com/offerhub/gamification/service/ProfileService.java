package com.offerhub.gamification.service;

import com.offerhub.gamification.dto.BadgeResponse;
import com.offerhub.gamification.dto.LeaderboardResponse;
import com.offerhub.gamification.dto.ProfileResponse;
import com.offerhub.gamification.entity.Badge;
import com.offerhub.gamification.entity.EarnedBadge;
import com.offerhub.gamification.entity.ExpertProfile;
import com.offerhub.gamification.entity.Level;
import com.offerhub.gamification.repository.EarnedBadgeRepository;
import com.offerhub.gamification.repository.ExpertProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Read side of the service: everything the profile and leaderboard screens need. */
@Service
@RequiredArgsConstructor
public class ProfileService {

    /** Case document 7.4: the board shows the first ten. */
    private static final int LEADERBOARD_SIZE = 10;

    private final ExpertProfileRepository profileRepository;
    private final EarnedBadgeRepository badgeRepository;
    private final LeaderboardService leaderboardService;

    /**
     * An expert with no scored work has no row yet, which is not an error - they see an
     * empty profile rather than a 404.
     */
    @Transactional(readOnly = true)
    public ProfileResponse profileOf(UUID expertId) {
        ExpertProfile profile = profileRepository.findById(expertId)
                .orElseGet(() -> ExpertProfile.empty(expertId));

        List<Badge> badges = badgeRepository.findByExpertIdOrderByEarnedAtAsc(expertId).stream()
                .map(EarnedBadge::getBadge)
                .toList();

        return new ProfileResponse(
                profile.getTotalPoints(),
                Level.fromPoints(profile.getTotalPoints()),
                badges,
                leaderboardService.rankOf(Period.DAILY, expertId),
                leaderboardService.rankOf(Period.WEEKLY, expertId),
                profile.getCasesResolved(),
                averagePoints(profile));
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse leaderboard(Period period) {
        return new LeaderboardResponse(period.getParam(),
                leaderboardService.top(period, LEADERBOARD_SIZE));
    }

    /** The whole catalog, so the client can show what is still locked. */
    @Transactional(readOnly = true)
    public List<BadgeResponse> badgesOf(UUID expertId) {
        Map<Badge, Instant> earned = badgeRepository.findByExpertIdOrderByEarnedAtAsc(expertId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        EarnedBadge::getBadge, EarnedBadge::getEarnedAt, (a, b) -> a));

        return Arrays.stream(Badge.values())
                .map(badge -> new BadgeResponse(badge, earned.containsKey(badge), earned.get(badge)))
                .toList();
    }

    /** Points per resolved case, one decimal. Zero cases would divide by zero. */
    private static BigDecimal averagePoints(ExpertProfile profile) {
        if (profile.getCasesResolved() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(profile.getTotalPoints())
                .divide(BigDecimal.valueOf(profile.getCasesResolved()), 1, RoundingMode.HALF_UP);
    }
}
