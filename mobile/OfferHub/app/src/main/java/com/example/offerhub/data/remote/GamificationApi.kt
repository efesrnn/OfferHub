package com.example.offerhub.data.remote

import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.remote.dto.BadgeDto
import com.example.offerhub.data.remote.dto.GamificationProfileDto
import com.example.offerhub.data.remote.dto.LeaderboardDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GamificationApi {
    @GET("api/v1/game/profile")
    suspend fun getProfile(): Response<ApiResponse<GamificationProfileDto>>

    @GET("api/v1/game/leaderboard")
    suspend fun getLeaderboard(
        @Query("period") period: String
    ): Response<ApiResponse<LeaderboardDto>>

    @GET("api/v1/game/badges")
    suspend fun getBadges(): Response<ApiResponse<List<BadgeDto>>>
}
