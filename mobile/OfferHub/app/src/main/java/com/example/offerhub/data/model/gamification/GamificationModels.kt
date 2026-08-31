package com.example.offerhub.data.model.gamification

enum class ExpertLevel {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM
}

enum class RankingPeriod {
    DAILY,
    WEEKLY
}

data class ExpertBadge(
    val id: String,
    val title: String
)

data class GamificationProfile(
    val expertId: String,
    val totalPoints: Int,
    val level: ExpertLevel,
    val completedCases: Int,
    val averageRating: Double,
    val dailyRank: Int?,
    val weeklyRank: Int?,
    val badges: List<ExpertBadge>
)

data class RankingEntry(
    val expertId: String,
    val displayName: String,
    val points: Int,
    val rank: Int
)
