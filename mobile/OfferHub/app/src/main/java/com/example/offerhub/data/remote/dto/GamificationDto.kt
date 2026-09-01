package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.gamification.ExpertBadge
import com.example.offerhub.data.model.gamification.ExpertLevel
import com.example.offerhub.data.model.gamification.GamificationProfile
import com.example.offerhub.data.model.gamification.RankingEntry

data class GamificationProfileDto(
    val totalPoints: Int?,
    val level: String?,
    val badges: List<String>?,
    val dailyRank: Long?,
    val weeklyRank: Long?,
    val casesResolved: Int?,
    val avgPointsPerCase: Double?
)

data class LeaderboardDto(
    val period: String?,
    val items: List<LeaderboardEntryDto>?
)

data class LeaderboardEntryDto(
    val rank: Int?,
    val expertId: String?,
    val name: String?,
    val points: Int?
)

data class BadgeDto(
    val badge: String?,
    val earned: Boolean?,
    val earnedAt: String?
)

fun GamificationProfileDto.toDomain(expertId: String): GamificationProfile? {
    if (expertId.isBlank()) return null

    return GamificationProfile(
        expertId = expertId,
        totalPoints = totalPoints ?: return null,
        level = level.toExpertLevel() ?: return null,
        completedCases = casesResolved ?: return null,
        averagePointsPerCase = avgPointsPerCase ?: return null,
        dailyRank = dailyRank?.toIntSafely(),
        weeklyRank = weeklyRank?.toIntSafely(),
        badges = badges.orEmpty().mapNotNull { it.toExpertBadge() }
    )
}

fun LeaderboardEntryDto.toDomain(): RankingEntry? {
    val safeExpertId = expertId?.takeIf(String::isNotBlank) ?: return null
    val safeRank = rank ?: return null
    val safePoints = points ?: return null

    return RankingEntry(
        expertId = safeExpertId,
        displayName = name?.takeIf(String::isNotBlank)
            ?: "Expert #${safeExpertId.take(8)}",
        points = safePoints,
        rank = safeRank
    )
}

fun BadgeDto.toDomain(): ExpertBadge? = badge?.toExpertBadge(
    earned = earned ?: false,
    earnedAt = earnedAt
)

private fun String?.toExpertLevel(): ExpertLevel? = when (this) {
    "BRONZ" -> ExpertLevel.BRONZE
    "GUMUS" -> ExpertLevel.SILVER
    "ALTIN" -> ExpertLevel.GOLD
    "PLATIN" -> ExpertLevel.PLATINUM
    else -> null
}

private fun String.toExpertBadge(
    earned: Boolean = true,
    earnedAt: String? = null
): ExpertBadge? {
    val title = when (this) {
        "ILK_KAMPANYA" -> "First Campaign"
        "HIZ_USTASI" -> "Speed Master"
        "DONUSUM_KRALI" -> "Conversion King"
        "MARATONCU" -> "Marathoner"
        "CHURN_AVCISI" -> "Churn Hunter"
        "UZMAN" -> "Expert"
        else -> return null
    }
    return ExpertBadge(id = this, title = title, earned = earned, earnedAt = earnedAt)
}

private fun Long.toIntSafely(): Int? = takeIf {
    it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
}?.toInt()
