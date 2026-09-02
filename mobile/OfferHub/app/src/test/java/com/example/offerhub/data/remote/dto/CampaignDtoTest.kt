package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.campaign.CampaignType
import com.example.offerhub.data.model.campaign.Segment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CampaignDtoTest {
    @Test
    fun `dto maps backend enums and nullable AI values safely`() {
        val campaign = CampaignDto(
            campaignNo = "CMP-2026-000001",
            title = "Summer Campaign",
            type = "EK_PAKET",
            targetSegment = "YUKSEK_DEGER",
            aiSegment = "BELIRSIZ",
            segment = "BELIRSIZ",
            discountRate = 20,
            validUntil = "2027-12-31T23:59:59Z",
            status = "YENI",
            priority = "ORTA",
            conversionProbability = null,
            createdAt = "2026-08-28T10:00:00Z"
        ).toDomain()!!

        assertEquals(CampaignType.EK_PAKET, campaign.type)
        assertEquals(Segment.YUKSEK_DEGER, campaign.targetSegment)
        assertNull(campaign.conversionProbability)
    }

    @Test
    fun `unknown enum maps to UNKNOWN instead of crashing`() {
        val campaign = CampaignDto(
            campaignNo = "CMP-2026-000001",
            title = "Future Campaign",
            type = "FUTURE_TYPE",
            targetSegment = "FUTURE_SEGMENT",
            aiSegment = null,
            segment = null,
            discountRate = 10,
            validUntil = "2027-12-31T23:59:59Z",
            status = "YENI",
            priority = "ORTA",
            conversionProbability = null,
            createdAt = "2026-08-28T10:00:00Z"
        ).toDomain()!!

        assertEquals(CampaignType.UNKNOWN, campaign.type)
        assertEquals(Segment.UNKNOWN, campaign.targetSegment)
    }

    @Test
    fun `missing required discount rejects response`() {
        val campaign = CampaignDto(
            campaignNo = "CMP-2026-000001",
            title = "Invalid Campaign",
            type = "EK_PAKET",
            targetSegment = "YUKSEK_DEGER",
            aiSegment = "BELIRSIZ",
            segment = "BELIRSIZ",
            discountRate = null,
            validUntil = "2027-12-31T23:59:59Z",
            status = "YENI",
            priority = "ORTA",
            conversionProbability = null,
            createdAt = "2026-08-28T10:00:00Z"
        ).toDomain()

        assertNull(campaign)
    }
}
