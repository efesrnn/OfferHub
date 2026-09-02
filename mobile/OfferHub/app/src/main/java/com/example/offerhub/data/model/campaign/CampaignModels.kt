package com.example.offerhub.data.model.campaign

enum class CampaignStatus { YENI, YAYINDA, ARSIVLENDI, UNKNOWN }

enum class CaseStatus { YENI, ATANDI, OPTIMIZE_EDILIYOR, TEST_EDILIYOR, TAMAMLANDI, YAYINDA, ARSIVLENDI, UNKNOWN }

enum class Priority { DUSUK, ORTA, YUKSEK, KRITIK, UNKNOWN }

enum class Segment { YUKSEK_DEGER, RISKLI_KAYIP, YENI_ABONE, PASIF, BELIRSIZ, UNKNOWN }

enum class CampaignType { EK_PAKET, TARIFE_YUKSELTME, CIHAZ_FIRSATI, SADAKAT, UNKNOWN }

data class Campaign(
    val campaignNo: String,
    val title: String,
    val type: CampaignType,
    val targetSegment: Segment,
    val segment: Segment,
    val discountRate: Int,
    val validUntil: String,
    val status: CampaignStatus,
    val priority: Priority,
    val aiSegment: Segment,
    val conversionProbability: Double?,
    val createdAt: String
)

data class OptimizationCase(
    val caseId: String,
    val campaignNo: String,
    val title: String,
    val segment: Segment,
    val aiSegment: Segment,
    val priority: Priority,
    val status: CaseStatus,
    val conversionProbability: Double?,
    val recommendationScore: Double?,
    val slaDeadline: String?,
    val slaRemainingSeconds: Long?,
    val optimizationNote: String?,
    val assignedExpertId: String?,
    val createdAt: String,
    val completedAt: String?
)
