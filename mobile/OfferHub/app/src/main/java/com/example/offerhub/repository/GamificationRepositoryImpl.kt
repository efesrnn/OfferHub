package com.example.offerhub.repository

import com.example.offerhub.data.model.gamification.GamificationProfile
import com.example.offerhub.data.model.gamification.RankingEntry
import com.example.offerhub.data.model.gamification.RankingPeriod
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.remote.GamificationApi
import com.example.offerhub.data.remote.dto.toDomain
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

class GamificationRepositoryImpl(
    private val api: GamificationApi
) : GamificationRepository {

    override suspend fun getProfile(expertId: String): GamificationResult<GamificationProfile> {
        val profileResult = call({ api.getProfile() }) { dto -> dto.toDomain(expertId) }
        if (profileResult !is GamificationResult.Success) return profileResult

        val badgesResult = call({ api.getBadges() }) { badgeDtos ->
            badgeDtos.mapNotNull { it.toDomain() }
        }

        return if (badgesResult is GamificationResult.Success) {
            GamificationResult.Success(
                profileResult.value.copy(badges = badgesResult.value)
            )
        } else {
            profileResult
        }
    }

    override suspend fun getRanking(
        period: RankingPeriod,
        currentExpertId: String
    ): GamificationResult<List<RankingEntry>> = call(
        request = { api.getLeaderboard(period.name.lowercase()) }
    ) { dto ->
        dto.items?.mapNotNull { it.toDomain() }
    }

    private suspend fun <Dto, Domain> call(
        request: suspend () -> Response<ApiResponse<Dto>>,
        transform: (Dto) -> Domain?
    ): GamificationResult<Domain> = try {
        val response = request()
        val envelope = response.body()
        val value = envelope?.data?.let(transform)

        if (response.isSuccessful && envelope?.success == true && value != null) {
            GamificationResult.Success(value)
        } else {
            GamificationResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        GamificationResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        GamificationResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    private fun errorFrom(response: Response<*>, bodyError: ApiError?): ApiError {
        if (bodyError != null) return bodyError
        return runCatching {
            Gson().fromJson(response.errorBody()?.string(), ErrorEnvelope::class.java).error
        }.getOrNull() ?: ApiError("UNKNOWN_ERROR")
    }

    private data class ErrorEnvelope(val error: ApiError?)
}
