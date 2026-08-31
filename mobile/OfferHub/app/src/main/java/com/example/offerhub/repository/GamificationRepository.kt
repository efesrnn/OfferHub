package com.example.offerhub.repository

import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.model.gamification.GamificationProfile
import com.example.offerhub.data.model.gamification.RankingEntry
import com.example.offerhub.data.model.gamification.RankingPeriod

sealed interface GamificationResult<out T> {
    data class Success<T>(val value: T) : GamificationResult<T>
    data class Failure(val error: ApiError) : GamificationResult<Nothing>
}

interface GamificationRepository {
    suspend fun getProfile(expertId: String): GamificationResult<GamificationProfile>

    suspend fun getRanking(
        period: RankingPeriod,
        currentExpertId: String
    ): GamificationResult<List<RankingEntry>>
}
