package com.example.offerhub.data.remote

import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.network.PagedResult
import com.example.offerhub.data.remote.dto.AssignCaseRequest
import com.example.offerhub.data.remote.dto.CaseDto
import com.example.offerhub.data.remote.dto.StatusChangeRequest
import com.example.offerhub.data.remote.dto.CampaignDto
import com.example.offerhub.data.remote.dto.CreateCampaignRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ExpertApi {
    @POST("api/v1/campaigns")
    suspend fun createCampaign(
        @Body request: CreateCampaignRequest
    ): Response<ApiResponse<CampaignDto>>

    @GET("api/v1/campaigns")
    suspend fun getCampaigns(
        @Query("status") status: String? = null,
        @Query("segment") segment: String? = null,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiResponse<PagedResult<CampaignDto>>>

    @GET("api/v1/campaigns/{campaignNo}")
    suspend fun getCampaignDetail(
        @Path("campaignNo") campaignNo: String
    ): Response<ApiResponse<CampaignDto>>

    @GET("api/v1/cases")
    suspend fun getCases(
        @Query("status") status: String? = null,
        @Query("assignedTo") assignedTo: String? = null,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiResponse<PagedResult<CaseDto>>>

    @GET("api/v1/cases/{caseId}")
    suspend fun getCaseDetail(
        @Path("caseId") caseId: String
    ): Response<ApiResponse<CaseDto>>

    @PATCH("api/v1/cases/{caseId}/status")
    suspend fun changeCaseStatus(
        @Path("caseId") caseId: String,
        @Body request: StatusChangeRequest
    ): Response<ApiResponse<CaseDto>>

    @POST("api/v1/cases/{caseId}/assign")
    suspend fun assignCase(
        @Path("caseId") caseId: String,
        @Body request: AssignCaseRequest
    ): Response<ApiResponse<CaseDto>>
}
