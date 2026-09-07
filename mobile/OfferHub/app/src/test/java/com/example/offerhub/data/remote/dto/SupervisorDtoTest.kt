package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.campaign.Segment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupervisorDtoTest {
    @Test
    fun `dashboard dto maps conversion trend percentages`() {
        val dto = dashboardDto(
            conversionTrend = listOf(
                ConversionTrendPointDto("2026-09-03", 0.34, 15)
            )
        )

        val trend = dto.toConversionTrend().single()

        assertEquals("2026-09-03", trend.period)
        assertEquals(34.0, trend.conversionPercent, 0.001)
        assertEquals(15L, trend.answeredOfferCount)
    }

    @Test
    fun `dashboard dto preserves unmeasured expert conversion lift`() {
        val dto = dashboardDto(
            expertPerformance = listOf(
                ExpertPerformanceDto(
                    expertId = "expert-1",
                    completedCases = 4,
                    averageConversionLift = null,
                    averageCompletionHours = 2.5,
                    activeCaseCount = 3
                )
            )
        )

        val expert = dto.toExpertPerformance().single()

        assertEquals("expert-1", expert.expertId)
        assertNull(expert.averageConversionIncrease)
        assertEquals(4, expert.completedCases)
        assertEquals(3, expert.activeCaseCount)
    }

    @Test
    fun `dashboard dto maps segment counts`() {
        val dto = dashboardDto(
            segmentDistribution = mapOf("RISKLI_KAYIP" to 18L)
        )

        val segment = dto.toSegmentDistribution().single()

        assertEquals(Segment.RISKLI_KAYIP, segment.segment)
        assertEquals(18, segment.campaignCount)
    }

    private fun dashboardDto(
        segmentDistribution: Map<String, Long> = emptyMap(),
        conversionTrend: List<ConversionTrendPointDto> = emptyList(),
        expertPerformance: List<ExpertPerformanceDto> = emptyList()
    ) = SupervisorDashboardDto(
        segmentDistribution = segmentDistribution,
        conversionRate = 0.34,
        slaComplianceRate = 0.91,
        slaBreachedActiveCases = 3,
        pendingQueueCount = 5,
        aiAccuracyRate = 0.78,
        aiClassifiedCampaigns = 42,
        conversionTrend = conversionTrend,
        expertPerformance = expertPerformance
    )
}
