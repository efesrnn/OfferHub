package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.OptimizationCase
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment

data class CaseDto(
    val caseId: String?,
    val campaignNo: String?,
    val title: String?,
    val segment: String?,
    val aiSegment: String?,
    val priority: String?,
    val status: String?,
    val conversionProbability: Double?,
    val recommendationScore: Double?,
    val slaDeadline: String?,
    val slaRemainingSeconds: Long?,
    val optimizationNote: String?,
    val assignedExpertId: String?,
    val createdAt: String?,
    val completedAt: String?
)

fun CaseDto.toDomain(): OptimizationCase? {
    val safeCaseId = caseId?.takeIf { it.isNotBlank() } ?: return null
    val safeCampaignNo = campaignNo?.takeIf { it.isNotBlank() } ?: return null
    val safeTitle = title?.takeIf { it.isNotBlank() } ?: return null
    val safeCreatedAt = createdAt?.takeIf { it.isNotBlank() } ?: return null

    return OptimizationCase(
        caseId = safeCaseId,
        campaignNo = safeCampaignNo,
        title = safeTitle,
        segment = enumValueOrUnknown(segment, Segment.UNKNOWN),
        aiSegment = enumValueOrUnknown(aiSegment, Segment.UNKNOWN),
        priority = enumValueOrUnknown(priority, Priority.UNKNOWN),
        status = enumValueOrUnknown(status, CaseStatus.UNKNOWN),
        conversionProbability = conversionProbability,
        recommendationScore = recommendationScore,
        slaDeadline = slaDeadline,
        slaRemainingSeconds = slaRemainingSeconds,
        optimizationNote = optimizationNote,
        assignedExpertId = assignedExpertId,
        createdAt = safeCreatedAt,
        completedAt = completedAt
    )
}

private inline fun <reified T : Enum<T>> enumValueOrUnknown(
    value: String?,
    unknown: T
): T = value
    ?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
    ?: unknown
