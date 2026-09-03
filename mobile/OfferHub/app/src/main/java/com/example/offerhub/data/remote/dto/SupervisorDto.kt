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
    val pendingQueueCount: Long?
)

data class AiAccuracyDto(
    val overallAccuracy: Double?,
    val bySegment: Map<String, Double>?
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
        slaRemainingSeconds = slaRemainingSeconds
    )
}

fun SupervisorDashboardDto.toSegmentDistribution(): List<SegmentDistribution> =
    segmentDistribution.orEmpty().map { (segment, count) ->
        SegmentDistribution(
            segment = segment.toEnumOrUnknown(Segment.UNKNOWN),
            campaignCount = count.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        )
    }

private inline fun <reified T : Enum<T>> String?.toEnumOrUnknown(unknown: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: unknown
