package com.example.offerhub.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.Priority

class MockSupervisorRepositoryTest {
    @Test
    fun `dashboard contains required supervisor metrics`() = runBlocking {
        val result = MockSupervisorRepository().getDashboard() as SupervisorResult.Success

        assertTrue(result.value.aiAccuracyPercent in 0.0..100.0)
        assertTrue(result.value.slaCompliancePercent in 0.0..100.0)
        assertTrue(result.value.segmentDistribution.isNotEmpty())
        assertTrue(result.value.conversionTrend.isNotEmpty())
        assertTrue(result.value.expertPerformance.isNotEmpty())
    }

    @Test
    fun `pending assignment count agrees with attention list examples`() = runBlocking {
        val result = MockSupervisorRepository().getDashboard() as SupervisorResult.Success

        assertEquals(2, result.value.attentionCases.count { it.assignedExpertId == null })
        assertTrue(result.value.pendingAssignmentCount >= 2)
    }

    @Test
    fun `assigning a pending case moves it to assigned state and updates capacity`() = runBlocking {
        val repository = MockSupervisorRepository()

        val result = repository.assignCase("case-102", "expert-1") as SupervisorResult.Success
        val assignedCase = result.value.attentionCases.first { it.caseId == "case-102" }

        assertEquals(CaseStatus.ATANDI, assignedCase.status)
        assertEquals("expert-1", assignedCase.assignedExpertId)
        assertEquals(9, result.value.expertPerformance.first { it.expertId == "expert-1" }.activeCaseCount)
    }

    @Test
    fun `expert at capacity cannot receive another case`() = runBlocking {
        val result = MockSupervisorRepository().assignCase("case-102", "expert-2")

        assertTrue(result is SupervisorResult.Failure)
        assertEquals("EXPERT_CAPACITY_FULL", (result as SupervisorResult.Failure).error.code)
    }

    @Test
    fun `completed case can be published`() = runBlocking {
        val repository = MockSupervisorRepository()

        val result = repository.publishCase("case-106") as SupervisorResult.Success

        assertEquals(CaseStatus.YAYINDA, result.value.attentionCases.first { it.caseId == "case-106" }.status)
    }

    @Test
    fun `classification cannot change after testing starts`() = runBlocking {
        val result = MockSupervisorRepository().updateCaseClassification(
            "case-105",
            Segment.PASIF,
            Priority.ORTA
        )

        assertTrue(result is SupervisorResult.Failure)
        assertEquals("CASE_CLASSIFICATION_LOCKED", (result as SupervisorResult.Failure).error.code)
    }
}
