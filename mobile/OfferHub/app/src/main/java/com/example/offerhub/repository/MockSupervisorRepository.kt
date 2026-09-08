package com.example.offerhub.repository

import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.supervisor.ConversionTrendPoint
import com.example.offerhub.data.model.supervisor.ExpertPerformanceSummary
import com.example.offerhub.data.model.supervisor.SegmentDistribution
import com.example.offerhub.data.model.supervisor.SupervisorCaseSummary
import com.example.offerhub.data.model.supervisor.SupervisorDashboard
import kotlinx.coroutines.delay
import java.time.LocalDate

class MockSupervisorRepository : SupervisorRepository {
    private var dashboard = SupervisorDashboard(
        aiAccuracyPercent = 87.4,
        aiClassifiedCampaignCount = 55,
        conversionRatePercent = 34.0,
        slaCompliancePercent = 92.1,
        slaBreachedActiveCaseCount = 1,
        activeCaseCount = 3,
        pendingAssignmentCount = 2,
        segmentDistribution = listOf(
            SegmentDistribution(Segment.RISKLI_KAYIP, 18), SegmentDistribution(Segment.YUKSEK_DEGER, 14),
            SegmentDistribution(Segment.YENI_ABONE, 10), SegmentDistribution(Segment.PASIF, 7),
            SegmentDistribution(Segment.BELIRSIZ, 6)
        ),
        conversionTrend = listOf(
            18.0 to 11L, 20.5 to 14L, 19.2 to 13L, 23.1 to 17L,
            25.8 to 19L, 24.6 to 18L, 28.4 to 21L, 30.2 to 23L,
            27.9 to 20L, 32.5 to 25L, 34.1 to 27L, 33.6 to 26L,
            36.8 to 30L, 39.4 to 33L
        ).mapIndexed { index, (conversionPercent, answeredCount) ->
            ConversionTrendPoint(
                period = LocalDate.now().minusDays((13 - index).toLong()).toString(),
                conversionPercent = conversionPercent,
                answeredOfferCount = answeredCount
            )
        },
        attentionCases = listOf(
            SupervisorCaseSummary("case-101", "Churn Recovery", Priority.KRITIK, CaseStatus.ATANDI, Segment.RISKLI_KAYIP, "expert-1", -900),
            SupervisorCaseSummary("case-102", "High Value Retention", Priority.YUKSEK, CaseStatus.YENI, Segment.BELIRSIZ, null, null),
            SupervisorCaseSummary("case-103", "Passive Subscriber Win-back", Priority.YUKSEK, CaseStatus.YENI, Segment.PASIF, null, 3_600),
            SupervisorCaseSummary("case-104", "Tariff Upgrade Test", Priority.ORTA, CaseStatus.OPTIMIZE_EDILIYOR, Segment.YUKSEK_DEGER, "expert-2", 18_000),
            SupervisorCaseSummary("case-105", "New Subscriber Offer", Priority.ORTA, CaseStatus.TEST_EDILIYOR, Segment.YENI_ABONE, "expert-3", 24_000),
            SupervisorCaseSummary("case-106", "Loyalty Campaign Review", Priority.DUSUK, CaseStatus.TAMAMLANDI, Segment.YUKSEK_DEGER, "expert-2", 36_000),
            SupervisorCaseSummary("case-107", "Published Device Campaign", Priority.ORTA, CaseStatus.YAYINDA, Segment.YENI_ABONE, "expert-3", 48_000)
        ),
        expertPerformance = listOf(
            ExpertPerformanceSummary("expert-1", "Ayse Yilmaz", 34, 12.4, 3.2, 8),
            ExpertPerformanceSummary("expert-2", "Can Demir", 29, 10.8, 3.8, 10),
            ExpertPerformanceSummary("expert-3", "Ece Kaya", 27, 9.7, 4.1, 6)
        )
    )

    override suspend fun getDashboard(): SupervisorResult<SupervisorDashboard> {
        delay(250)
        return SupervisorResult.Success(dashboard)
    }

    override suspend fun assignCase(caseId: String, expertId: String): SupervisorResult<SupervisorDashboard> {
        delay(200)
        val targetCase = dashboard.attentionCases.firstOrNull { it.caseId == caseId }
            ?: return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("CASE_NOT_FOUND"))
        if (targetCase.assignedExpertId != null || targetCase.status != CaseStatus.YENI) {
            return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("CASE_ALREADY_ASSIGNED"))
        }
        val expert = dashboard.expertPerformance.firstOrNull { it.expertId == expertId }
            ?: return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("EXPERT_NOT_FOUND"))
        if (expert.activeCaseCount >= expert.maximumCaseCapacity) {
            return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("EXPERT_CAPACITY_FULL"))
        }
        dashboard = dashboard.copy(
            activeCaseCount = dashboard.activeCaseCount + 1,
            pendingAssignmentCount = (dashboard.pendingAssignmentCount - 1).coerceAtLeast(0),
            attentionCases = dashboard.attentionCases.map {
                if (it.caseId == caseId) it.copy(assignedExpertId = expertId, status = CaseStatus.ATANDI) else it
            },
            expertPerformance = dashboard.expertPerformance.map {
                if (it.expertId == expertId) it.copy(activeCaseCount = it.activeCaseCount + 1) else it
            }
        )
        return SupervisorResult.Success(dashboard)
    }

    override suspend fun publishCase(caseId: String): SupervisorResult<SupervisorDashboard> {
        delay(200)
        val targetCase = dashboard.attentionCases.firstOrNull { it.caseId == caseId }
            ?: return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("CASE_NOT_FOUND"))
        if (targetCase.status != CaseStatus.TAMAMLANDI) {
            return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("INVALID_STATUS_TRANSITION"))
        }
        dashboard = dashboard.copy(attentionCases = dashboard.attentionCases.map {
            if (it.caseId == caseId) it.copy(status = CaseStatus.YAYINDA) else it
        })
        return SupervisorResult.Success(dashboard)
    }

    override suspend fun updateCaseClassification(
        campaignNo: String,
        segment: Segment,
        priority: Priority,
        reason: String
    ): SupervisorResult<SupervisorDashboard> {
        delay(200)
        if (reason.isBlank()) {
            return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("VALIDATION_ERROR"))
        }
        if (segment == Segment.RISKLI_KAYIP && priority !in setOf(Priority.YUKSEK, Priority.KRITIK)) {
            return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("RISK_SEGMENT_PRIORITY_TOO_LOW"))
        }
        if (dashboard.attentionCases.none { it.campaignNo == campaignNo }) {
            return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("CASE_NOT_FOUND"))
        }
        val targetCase = dashboard.attentionCases.first { it.campaignNo == campaignNo }
        if (targetCase.status !in setOf(CaseStatus.YENI, CaseStatus.ATANDI, CaseStatus.OPTIMIZE_EDILIYOR)) {
            return SupervisorResult.Failure(com.example.offerhub.data.network.ApiError("CASE_CLASSIFICATION_LOCKED"))
        }
        dashboard = dashboard.copy(attentionCases = dashboard.attentionCases.map {
            if (it.campaignNo == campaignNo) it.copy(segment = segment, priority = priority) else it
        })
        return SupervisorResult.Success(dashboard)
    }

    override suspend fun getExperts(): SupervisorResult<List<AdminStaff>> {
        delay(150)
        return SupervisorResult.Success(
            listOf(
                AdminStaff("expert-1", "Ayse", "Yilmaz", "ayse@offerhub.com", "EXPERT", emptyList(), emptyList()),
                AdminStaff("expert-2", "Can", "Demir", "can@offerhub.com", "EXPERT", emptyList(), emptyList()),
                AdminStaff("expert-3", "Ece", "Kaya", "ece@offerhub.com", "EXPERT", emptyList(), emptyList())
            )
        )
    }
}
