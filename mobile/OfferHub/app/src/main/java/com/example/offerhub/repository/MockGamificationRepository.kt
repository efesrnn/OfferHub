package com.example.offerhub.repository

import com.example.offerhub.data.model.gamification.ExpertBadge
import com.example.offerhub.data.model.gamification.ExpertLevel
import com.example.offerhub.data.model.gamification.GamificationProfile
import com.example.offerhub.data.model.gamification.RankingEntry
import com.example.offerhub.data.model.gamification.RankingPeriod
import com.example.offerhub.data.network.ApiError

class MockGamificationRepository : GamificationRepository {
    override suspend fun getProfile(expertId: String): GamificationResult<GamificationProfile> {
        if (expertId.isBlank()) return GamificationResult.Failure(ApiError("USER_NOT_FOUND"))

        return GamificationResult.Success(
            GamificationProfile(
                expertId = expertId,
                totalPoints = 2_140,
                level = ExpertLevel.GOLD,
                completedCases = 34,
                averagePointsPerCase = 11.2,
                dailyRank = 3,
                weeklyRank = 7,
                badges = listOf(
                    ExpertBadge("ILK_KAMPANYA", "First Campaign"),
                    ExpertBadge("CHURN_AVCISI", "Churn Hunter"),
                    ExpertBadge("HIZ_USTASI", "Speed Master"),
                    ExpertBadge("DONUSUM_KRALI", "Conversion King", earned = false),
                    ExpertBadge("MARATONCU", "Marathoner", earned = false),
                    ExpertBadge("UZMAN", "Expert", earned = false)
                )
            )
        )
    }

    override suspend fun getRanking(
        period: RankingPeriod,
        currentExpertId: String
    ): GamificationResult<List<RankingEntry>> {
        if (currentExpertId.isBlank()) return GamificationResult.Failure(ApiError("USER_NOT_FOUND"))

        val daily = listOf(
            Triple("expert-1", "Ayse Yilmaz", 320),
            Triple("expert-2", "Can Demir", 285),
            Triple(currentExpertId, "You", 260),
            Triple("expert-4", "Ece Kaya", 225),
            Triple("expert-5", "Mert Aydin", 190),
            Triple("expert-6", "Selin Ak", 165),
            Triple("expert-7", "Bora Cetin", 140),
            Triple("expert-8", "Deniz Arslan", 115),
            Triple("expert-9", "Elif Yildiz", 80),
            Triple("expert-10", "Kerem Sahin", 0)
        )
        val weekly = listOf(
            Triple("expert-1", "Ayse Yilmaz", 1_420),
            Triple("expert-4", "Ece Kaya", 1_305),
            Triple("expert-2", "Can Demir", 1_260),
            Triple("expert-5", "Mert Aydin", 1_180),
            Triple("expert-6", "Selin Ak", 1_060),
            Triple("expert-7", "Bora Cetin", 980),
            Triple(currentExpertId, "You", 920),
            Triple("expert-8", "Deniz Arslan", 760),
            Triple("expert-9", "Elif Yildiz", 540),
            Triple("expert-10", "Kerem Sahin", 0)
        )

        val entries = (if (period == RankingPeriod.DAILY) daily else weekly)
            .mapIndexed { index, (id, name, points) ->
                RankingEntry(
                    expertId = id,
                    displayName = name,
                    points = points,
                    rank = index + 1
                )
            }
        return GamificationResult.Success(entries)
    }
}
