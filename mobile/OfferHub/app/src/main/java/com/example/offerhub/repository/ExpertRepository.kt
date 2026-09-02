package com.example.offerhub.repository

import com.example.offerhub.data.model.campaign.OptimizationCase
import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.Campaign
import com.example.offerhub.data.model.campaign.CampaignType
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.CampaignStatus
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.PagedResult

sealed interface ExpertResult<out T> {
    data class Success<T>(val value: T) : ExpertResult<T>
    data class Failure(val error: ApiError) : ExpertResult<Nothing>
}

interface ExpertRepository {
    suspend fun getCampaigns(
        page: Int,
        size: Int,
        status: CampaignStatus? = null,
        segment: Segment? = null
    ): ExpertResult<PagedResult<Campaign>>

    suspend fun getCampaignDetail(campaignNo: String): ExpertResult<Campaign>

    suspend fun createCampaign(
        title: String,
        type: CampaignType,
        targetSegment: Segment,
        discountRate: Int,
        validUntil: String
    ): ExpertResult<Campaign>

    suspend fun getAssignedCases(
        page: Int,
        size: Int,
        status: CaseStatus? = null
    ): ExpertResult<PagedResult<OptimizationCase>>

    suspend fun getCaseDetail(caseId: String): ExpertResult<OptimizationCase>

    suspend fun changeCaseStatus(
        caseId: String,
        targetStatus: CaseStatus,
        optimizationNote: String? = null
    ): ExpertResult<OptimizationCase>
}
