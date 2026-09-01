package com.offerhub.gamification.dto;

import com.offerhub.gamification.entity.Badge;
import com.offerhub.gamification.entity.Level;

import java.math.BigDecimal;
import java.util.List;

public record ProfileResponse(
        int totalPoints,
        Level level,
        List<Badge> badges,
        Long dailyRank,
        Long weeklyRank,
        int casesResolved,
        BigDecimal avgPointsPerCase
) {
}
