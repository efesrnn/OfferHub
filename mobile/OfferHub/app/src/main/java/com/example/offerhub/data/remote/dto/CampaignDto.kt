package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.campaign.Campaign
import com.example.offerhub.data.model.campaign.CampaignStatus
import com.example.offerhub.data.model.campaign.CampaignType
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment

data class CampaignDto(
    val campaignNo: String?,
    val title: String?,
    val type: String?,
    val targetSegment: String?,
    val aiSegment: String?,
    val segment: String?,
    val discountRate: Int?,
    val validUntil: String?,
    val status: String?,
    val priority: String?,
    val conversionProbability: Double?,
    val createdAt: String?
)

data class CreateCampaignRequest(
    val title: String,
    val type: String,
    val targetSegment: String,
    val discountRate: Int,
    val validUntil: String
)

fun CampaignDto.toDomain(): Campaign? {
    val safeCampaignNo = campaignNo?.takeIf(String::isNotBlank) ?: return null
    val safeTitle = title?.takeIf(String::isNotBlank) ?: return null
    val safeValidUntil = validUntil?.takeIf(String::isNotBlank) ?: return null
    val safeCreatedAt = createdAt?.takeIf(String::isNotBlank) ?: return null
    val safeDiscountRate = discountRate ?: return null

    return Campaign(
        campaignNo = safeCampaignNo,
        title = safeTitle,
        type = enumOrUnknown(type, CampaignType.UNKNOWN),
        targetSegment = enumOrUnknown(targetSegment, Segment.UNKNOWN),
        segment = enumOrUnknown(segment, Segment.UNKNOWN),
        aiSegment = enumOrUnknown(aiSegment, Segment.UNKNOWN),
        discountRate = safeDiscountRate,
        validUntil = safeValidUntil,
        status = enumOrUnknown(status, CampaignStatus.UNKNOWN),
        priority = enumOrUnknown(priority, Priority.UNKNOWN),
        conversionProbability = conversionProbability,
        createdAt = safeCreatedAt
    )
}

private inline fun <reified T : Enum<T>> enumOrUnknown(value: String?, unknown: T): T =
    value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: unknown
