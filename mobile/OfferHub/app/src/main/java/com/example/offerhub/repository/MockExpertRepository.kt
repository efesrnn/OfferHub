package com.example.offerhub.repository

import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.OptimizationCase
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.Campaign
import com.example.offerhub.data.model.campaign.CampaignStatus
import com.example.offerhub.data.model.campaign.CampaignType
import com.example.offerhub.data.network.PagedResult
import com.example.offerhub.data.network.ApiError
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

class MockExpertRepository : ExpertRepository {
    private val campaigns = mutableListOf(
        Campaign(
            campaignNo = "CMP-2026-000001",
            title = "Churn Prevention Campaign",
            type = CampaignType.EK_PAKET,
            targetSegment = Segment.RISKLI_KAYIP,
            segment = Segment.RISKLI_KAYIP,
            discountRate = 20,
            validUntil = "2026-12-31T23:59:59Z",
            status = CampaignStatus.YENI,
            priority = Priority.YUKSEK,
            aiSegment = Segment.BELIRSIZ,
            conversionProbability = null,
            createdAt = Instant.now().toString()
        )
    )

    override suspend fun getCampaigns(
        page: Int,
        size: Int,
        status: CampaignStatus?,
        segment: Segment?
    ): ExpertResult<PagedResult<Campaign>> {
        delay(250)
        val filtered = campaigns.filter { campaign ->
            (status == null || campaign.status == status) &&
                (segment == null || campaign.segment == segment)
        }
        val fromIndex = page * size
        val items = if (fromIndex >= filtered.size) emptyList() else filtered.drop(fromIndex).take(size)
        return ExpertResult.Success(PagedResult(items, filtered.size.toLong(), page, size))
    }

    override suspend fun getCampaignDetail(campaignNo: String): ExpertResult<Campaign> {
        delay(200)
        return campaigns.firstOrNull { it.campaignNo == campaignNo }
            ?.let { ExpertResult.Success(it) }
            ?: ExpertResult.Failure(ApiError("NOT_FOUND"))
    }

    override suspend fun createCampaign(
        title: String,
        type: CampaignType,
        targetSegment: Segment,
        discountRate: Int,
        validUntil: String
    ): ExpertResult<Campaign> {
        delay(300)
        val normalizedTitle = title.trim()
        if (
            normalizedTitle.isEmpty() || normalizedTitle.length > 200 ||
            '<' in normalizedTitle || '>' in normalizedTitle ||
            type == CampaignType.UNKNOWN || targetSegment == Segment.UNKNOWN ||
            discountRate !in 0..100
        ) return ExpertResult.Failure(ApiError("VALIDATION_ERROR"))

        val campaign = Campaign(
            campaignNo = "CMP-2026-${(campaigns.size + 2).toString().padStart(6, '0')}",
            title = normalizedTitle,
            type = type,
            targetSegment = targetSegment,
            segment = Segment.BELIRSIZ,
            discountRate = discountRate,
            validUntil = validUntil,
            status = CampaignStatus.YENI,
            priority = Priority.ORTA,
            aiSegment = Segment.BELIRSIZ,
            conversionProbability = null,
            createdAt = Instant.now().toString()
        )
        campaigns.add(0, campaign)
        return ExpertResult.Success(campaign)
    }

    private val cases = List(28) { index ->
        val priority = listOf(Priority.KRITIK, Priority.YUKSEK, Priority.ORTA, Priority.DUSUK)[index % 4]
        val remainingSeconds = when (priority) {
            Priority.KRITIK -> 3_600L + index * 120L
            Priority.YUKSEK -> 18_000L + index * 300L
            Priority.ORTA -> 54_000L + index * 600L
            Priority.DUSUK -> 180_000L + index * 900L
            Priority.UNKNOWN -> 0L
        }
        OptimizationCase(
            caseId = "case-${index + 1}",
            campaignNo = "CMP-2026-${(123 + index).toString().padStart(6, '0')}",
            title = "Optimization Campaign ${index + 1}",
            segment = listOf(Segment.RISKLI_KAYIP, Segment.YUKSEK_DEGER, Segment.YENI_ABONE, Segment.PASIF)[index % 4],
            aiSegment = listOf(Segment.RISKLI_KAYIP, Segment.YUKSEK_DEGER, Segment.YENI_ABONE, Segment.PASIF)[index % 4],
            priority = priority,
            status = listOf(CaseStatus.ATANDI, CaseStatus.OPTIMIZE_EDILIYOR, CaseStatus.TEST_EDILIYOR)[index % 3],
            conversionProbability = 0.25 + (index % 5) * 0.08,
            recommendationScore = 0.60 + (index % 4) * 0.07,
            slaDeadline = Instant.now().plus(remainingSeconds, ChronoUnit.SECONDS).toString(),
            slaRemainingSeconds = remainingSeconds,
            optimizationNote = null,
            assignedExpertId = "debug-expert",
            createdAt = Instant.now().minus(index.toLong(), ChronoUnit.HOURS).toString(),
            completedAt = null
        )
    }.sortedWith(
        compareBy<OptimizationCase> { priorityRank(it.priority) }
            .thenBy { it.slaRemainingSeconds ?: Long.MAX_VALUE }
    ).toMutableList()

    override suspend fun getAssignedCases(
        page: Int,
        size: Int,
        status: CaseStatus?
    ): ExpertResult<PagedResult<OptimizationCase>> {
        delay(350)
        val filtered = cases.filter { status == null || it.status == status }
        val fromIndex = page * size
        val items = if (fromIndex >= filtered.size) emptyList() else filtered.drop(fromIndex).take(size)
        return ExpertResult.Success(PagedResult(items, filtered.size.toLong(), page, size))
    }

    override suspend fun getCaseDetail(caseId: String): ExpertResult<OptimizationCase> {
        delay(250)
        return cases.firstOrNull { it.caseId == caseId }
            ?.let { ExpertResult.Success(it) }
            ?: ExpertResult.Failure(ApiError("NOT_FOUND"))
    }

    override suspend fun changeCaseStatus(
        caseId: String,
        targetStatus: CaseStatus,
        optimizationNote: String?
    ): ExpertResult<OptimizationCase> {
        delay(350)
        val index = cases.indexOfFirst { it.caseId == caseId }
        if (index == -1) return ExpertResult.Failure(ApiError("NOT_FOUND"))

        val current = cases[index]
        if (targetStatus !in allowedTargets(current.status)) {
            return ExpertResult.Failure(ApiError("INVALID_STATE_TRANSITION"))
        }
        val normalizedNote = optimizationNote?.trim()
        if (targetStatus == CaseStatus.TAMAMLANDI && normalizedNote.isNullOrEmpty()) {
            return ExpertResult.Failure(ApiError("OPTIMIZATION_NOTE_REQUIRED"))
        }
        if (normalizedNote != null && (normalizedNote.length > 1000 || '<' in normalizedNote || '>' in normalizedNote)) {
            return ExpertResult.Failure(ApiError("VALIDATION_ERROR"))
        }

        val updated = current.copy(
            status = targetStatus,
            optimizationNote = normalizedNote ?: current.optimizationNote,
            completedAt = if (targetStatus == CaseStatus.TAMAMLANDI) Instant.now().toString() else current.completedAt
        )
        cases[index] = updated
        return ExpertResult.Success(updated)
    }

    private fun priorityRank(priority: Priority): Int = when (priority) {
        Priority.KRITIK -> 0
        Priority.YUKSEK -> 1
        Priority.ORTA -> 2
        Priority.DUSUK -> 3
        Priority.UNKNOWN -> 4
    }

    private fun allowedTargets(status: CaseStatus): Set<CaseStatus> = when (status) {
        CaseStatus.YENI -> setOf(CaseStatus.ATANDI)
        CaseStatus.ATANDI -> setOf(CaseStatus.OPTIMIZE_EDILIYOR)
        CaseStatus.OPTIMIZE_EDILIYOR -> setOf(CaseStatus.TEST_EDILIYOR, CaseStatus.TAMAMLANDI)
        CaseStatus.TEST_EDILIYOR -> setOf(CaseStatus.OPTIMIZE_EDILIYOR)
        CaseStatus.TAMAMLANDI -> setOf(CaseStatus.YAYINDA)
        CaseStatus.YAYINDA -> setOf(CaseStatus.ARSIVLENDI)
        CaseStatus.ARSIVLENDI, CaseStatus.UNKNOWN -> emptySet()
    }
}
