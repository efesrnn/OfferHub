package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.supervisor.SupervisorCaseSummary
import com.example.offerhub.data.model.supervisor.SegmentDistribution

data class SupervisorDashboardDto(
    val segmentDistribution: Map<String, Long>?,
    val conversionRate: Double?,
    val slaComplianceRate: Double?,
    val slaBreachedActiveCases: Long?,
    val pendingQueueCount: Long?,
    val aiAccuracyRate: Double?,
    val aiClassifiedCampaigns: Long?,
    val conversionTrend: List<ConversionTrendPointDto>?,
    val expertPerformance: List<ExpertPerformanceDto>?
)

data class ConversionTrendPointDto(
    val date: String?,
    val conversionRate: Double?,
    val answered: Long?
)

data class ExpertPerformanceDto(
    val expertId: String?,
    val completedCases: Long?,
    val averageConversionLift: Double?,
    val averageCompletionHours: Double?,
    val activeCaseCount: Long?
)

data class ClassificationRequest(
    val segment: String?,
    val type: String? = null,
    val priority: String?,
    val reason: String
)

fun CaseDto.toSupervisorSummary(): SupervisorCaseSummary? {
    val safeCaseId = caseId?.takeIf(String::isNotBlank) ?: return null
    val safeTitle = title?.takeIf(String::isNotBlank) ?: return null

    return SupervisorCaseSummary(
        caseId = safeCaseId,
        title = safeTitle,
        priority = priority.toEnumOrUnknown(Priority.UNKNOWN),
        status = status.toEnumOrUnknown(CaseStatus.UNKNOWN),
        segment = segment.toEnumOrUnknown(Segment.UNKNOWN),
        assignedExpertId = assignedExpertId,
        slaRemainingSeconds = slaRemainingSeconds,
        campaignNo = campaignNo?.takeIf(String::isNotBlank) ?: return null
    )
}

fun SupervisorDashboardDto.toSegmentDistribution(): List<SegmentDistribution> =
    segmentDistribution.orEmpty().map { (segment, count) ->
        SegmentDistribution(
            segment = segment.toEnumOrUnknown(Segment.UNKNOWN),
            campaignCount = count.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        )
    }

fun SupervisorDashboardDto.toConversionTrend(): List<com.example.offerhub.data.model.supervisor.ConversionTrendPoint> =
    conversionTrend.orEmpty().mapNotNull { point ->
        val date = point.date?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val rate = point.conversionRate ?: return@mapNotNull null
        com.example.offerhub.data.model.supervisor.ConversionTrendPoint(
            period = date,
            conversionPercent = rate.toPercent(),
            answeredOfferCount = point.answered?.coerceAtLeast(0) ?: 0
        )
    }

fun SupervisorDashboardDto.toExpertPerformance(): List<com.example.offerhub.data.model.supervisor.ExpertPerformanceSummary> =
    expertPerformance.orEmpty().mapNotNull { expert ->
        val expertId = expert.expertId?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val completedCases = expert.completedCases ?: return@mapNotNull null
        val completionHours = expert.averageCompletionHours ?: return@mapNotNull null
        val activeCases = expert.activeCaseCount ?: return@mapNotNull null
        com.example.offerhub.data.model.supervisor.ExpertPerformanceSummary(
            expertId = expertId,
            displayName = expertId,
            completedCases = completedCases.toSafeInt(),
            averageConversionIncrease = expert.averageConversionLift?.toPercent(),
            averageCompletionHours = completionHours,
            activeCaseCount = activeCases.toSafeInt()
        )
    }

private fun Double.toPercent(): Double = if (this in 0.0..1.0) this * 100.0 else this

private fun Long.toSafeInt(): Int = coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

private inline fun <reified T : Enum<T>> String?.toEnumOrUnknown(unknown: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: unknown
