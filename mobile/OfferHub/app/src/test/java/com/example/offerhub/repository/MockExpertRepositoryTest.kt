package com.example.offerhub.repository

import com.example.offerhub.data.model.campaign.CampaignType
import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.Segment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockExpertRepositoryTest {
    @Test
    fun `assigned case can move to optimizing`() = runBlocking {
        val repository = MockExpertRepository()
        val cases = (repository.getAssignedCases(0, 20) as ExpertResult.Success).value.items
        val assigned = cases.first { it.status == CaseStatus.ATANDI }

        val result = repository.changeCaseStatus(assigned.caseId, CaseStatus.OPTIMIZE_EDILIYOR)

        assertEquals(CaseStatus.OPTIMIZE_EDILIYOR, (result as ExpertResult.Success).value.status)
    }

    @Test
    fun `completing optimization requires a note`() = runBlocking {
        val repository = MockExpertRepository()
        val cases = (repository.getAssignedCases(0, 20) as ExpertResult.Success).value.items
        val optimizing = cases.first { it.status == CaseStatus.OPTIMIZE_EDILIYOR }

        val result = repository.changeCaseStatus(optimizing.caseId, CaseStatus.TAMAMLANDI, "   ")

        assertTrue(result is ExpertResult.Failure)
        assertEquals("OPTIMIZATION_NOTE_REQUIRED", (result as ExpertResult.Failure).error.code)
    }

    @Test
    fun `valid campaign is created with generated campaign number`() = runBlocking {
        val repository = MockExpertRepository()

        val result = repository.createCampaign(
            title = "Summer Add-on",
            type = CampaignType.EK_PAKET,
            targetSegment = Segment.YUKSEK_DEGER,
            discountRate = 20,
            validUntil = "2027-12-31T23:59:59Z"
        )

        assertTrue(result is ExpertResult.Success)
        assertTrue((result as ExpertResult.Success).value.campaignNo.startsWith("CMP-"))
    }

    @Test
    fun `case status filter is applied before pagination`() = runBlocking {
        val repository = MockExpertRepository()

        val result = repository.getAssignedCases(
            page = 0,
            size = 20,
            status = CaseStatus.TEST_EDILIYOR
        ) as ExpertResult.Success

        assertTrue(result.value.items.isNotEmpty())
        assertTrue(result.value.items.all { it.status == CaseStatus.TEST_EDILIYOR })
    }

    @Test
    fun `campaign segment filter follows current segment like backend`() = runBlocking {
        val repository = MockExpertRepository()

        val result = repository.getCampaigns(
            page = 0,
            size = 20,
            segment = Segment.YUKSEK_DEGER
        ) as ExpertResult.Success

        assertTrue(result.value.items.isEmpty())
    }
}
