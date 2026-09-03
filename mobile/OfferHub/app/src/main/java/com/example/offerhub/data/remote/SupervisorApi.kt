package com.example.offerhub.data.remote

import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.network.PagedResult
import com.example.offerhub.data.remote.dto.AiAccuracyDto
import com.example.offerhub.data.remote.dto.AssignCaseRequest
import com.example.offerhub.data.remote.dto.CaseDto
import com.example.offerhub.data.remote.dto.SupervisorDashboardDto
import com.example.offerhub.data.remote.dto.StatusChangeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SupervisorApi {
    @GET("api/v1/campaigns/dashboard")
    suspend fun getDashboard(): Response<ApiResponse<SupervisorDashboardDto>>

    @GET("api/v1/cases")
    suspend fun getCases(
        @Query("status") status: String? = null,
        @Query("assignedTo") assignedTo: String? = null,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiResponse<PagedResult<CaseDto>>>

    @POST("api/v1/cases/{caseId}/assign")
    suspend fun assignCase(
        @Path("caseId") caseId: String,
        @Body request: AssignCaseRequest
    ): Response<ApiResponse<CaseDto>>

    @PATCH("api/v1/cases/{caseId}/status")
    suspend fun changeCaseStatus(
        @Path("caseId") caseId: String,
        @Body request: StatusChangeRequest
    ): Response<ApiResponse<CaseDto>>

    @GET("api/v1/ai/accuracy")
    suspend fun getAiAccuracy(): Response<ApiResponse<AiAccuracyDto>>
}
