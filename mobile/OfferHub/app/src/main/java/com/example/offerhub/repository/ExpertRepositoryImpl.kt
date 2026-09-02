package com.example.offerhub.repository

import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.OptimizationCase
import com.example.offerhub.data.model.campaign.Campaign
import com.example.offerhub.data.model.campaign.CampaignType
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.CampaignStatus
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.network.PagedResult
import com.example.offerhub.data.remote.ExpertApi
import com.example.offerhub.data.remote.dto.CaseDto
import com.example.offerhub.data.remote.dto.CampaignDto
import com.example.offerhub.data.remote.dto.CreateCampaignRequest
import com.example.offerhub.data.remote.dto.StatusChangeRequest
import com.example.offerhub.data.remote.dto.toDomain
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

class ExpertRepositoryImpl(
    private val api: ExpertApi
) : ExpertRepository {

    override suspend fun getCampaigns(
        page: Int,
        size: Int,
        status: CampaignStatus?,
        segment: Segment?
    ): ExpertResult<PagedResult<Campaign>> = try {
        val response = api.getCampaigns(
            status = status?.name,
            segment = segment?.name,
            page = page,
            size = size
        )
        val envelope = response.body()
        val data = envelope?.data
        val campaigns = data?.items?.mapNotNull { it.toDomain() }
        if (response.isSuccessful && envelope?.success == true && data != null && campaigns != null) {
            ExpertResult.Success(PagedResult(campaigns, data.total, data.page, data.size))
        } else {
            ExpertResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        ExpertResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        ExpertResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    override suspend fun getCampaignDetail(campaignNo: String): ExpertResult<Campaign> =
        campaignCall { api.getCampaignDetail(campaignNo) }

    override suspend fun createCampaign(
        title: String,
        type: CampaignType,
        targetSegment: Segment,
        discountRate: Int,
        validUntil: String
    ): ExpertResult<Campaign> = campaignCall {
        api.createCampaign(
            CreateCampaignRequest(
                title = title.trim(),
                type = type.name,
                targetSegment = targetSegment.name,
                discountRate = discountRate,
                validUntil = validUntil
            )
        )
    }

    override suspend fun getAssignedCases(
        page: Int,
        size: Int,
        status: CaseStatus?
    ): ExpertResult<PagedResult<OptimizationCase>> = try {
        val response = api.getCases(status = status?.name, page = page, size = size)
        val envelope = response.body()
        val data = envelope?.data
        val cases = data?.items?.mapNotNull { it.toDomain() }

        if (response.isSuccessful && envelope?.success == true && data != null && cases != null) {
            ExpertResult.Success(
                PagedResult(
                    items = cases,
                    total = data.total,
                    page = data.page,
                    size = data.size
                )
            )
        } else {
            ExpertResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        ExpertResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        ExpertResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    override suspend fun getCaseDetail(caseId: String): ExpertResult<OptimizationCase> =
        caseCall { api.getCaseDetail(caseId) }

    override suspend fun changeCaseStatus(
        caseId: String,
        targetStatus: CaseStatus,
        optimizationNote: String?
    ): ExpertResult<OptimizationCase> = caseCall {
        api.changeCaseStatus(
            caseId = caseId,
            request = StatusChangeRequest(
                targetStatus = targetStatus.name,
                optimizationNote = optimizationNote?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    private suspend fun caseCall(
        block: suspend () -> Response<ApiResponse<CaseDto>>
    ): ExpertResult<OptimizationCase> = try {
        val response = block()
        val envelope = response.body()
        val optimizationCase = envelope?.data?.toDomain()

        if (response.isSuccessful && envelope?.success == true && optimizationCase != null) {
            ExpertResult.Success(optimizationCase)
        } else {
            ExpertResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        ExpertResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        ExpertResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    private suspend fun campaignCall(
        block: suspend () -> Response<ApiResponse<CampaignDto>>
    ): ExpertResult<Campaign> = try {
        val response = block()
        val envelope = response.body()
        val campaign = envelope?.data?.toDomain()
        if (response.isSuccessful && envelope?.success == true && campaign != null) {
            ExpertResult.Success(campaign)
        } else {
            ExpertResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        ExpertResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        ExpertResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    private fun errorFrom(response: Response<*>, bodyError: ApiError?): ApiError {
        if (bodyError != null) return bodyError
        return runCatching {
            Gson().fromJson(response.errorBody()?.string(), ErrorEnvelope::class.java).error
        }.getOrNull() ?: ApiError("UNKNOWN_ERROR")
    }

    private data class ErrorEnvelope(val error: ApiError?)
}
