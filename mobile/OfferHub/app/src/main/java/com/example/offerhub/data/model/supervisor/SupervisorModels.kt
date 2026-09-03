package com.example.offerhub.data.model.supervisor

import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.CaseStatus

data class SupervisorDashboard(
    val aiAccuracyPercent: Double,
    val conversionRatePercent: Double,
    val slaCompliancePercent: Double,
    val slaBreachedActiveCaseCount: Long,
    val activeCaseCount: Int,
    val pendingAssignmentCount: Int,
    val segmentDistribution: List<SegmentDistribution>,
    val conversionTrend: List<ConversionTrendPoint>,
    val attentionCases: List<SupervisorCaseSummary>,
    val expertPerformance: List<ExpertPerformanceSummary>
)

data class SegmentDistribution(val segment: Segment, val campaignCount: Int)

data class ConversionTrendPoint(val period: String, val conversionPercent: Double)

data class SupervisorCaseSummary(
    val caseId: String,
    val title: String,
    val priority: Priority,
    val status: CaseStatus,
    val segment: Segment,
    val assignedExpertId: String?,
    val slaRemainingSeconds: Long?
)

data class ExpertPerformanceSummary(
    val expertId: String,
    val displayName: String,
    val completedCases: Int,
    val averageConversionIncrease: Double,
    val averageCompletionHours: Double,
    val activeCaseCount: Int,
    val maximumCaseCapacity: Int = 10
)
