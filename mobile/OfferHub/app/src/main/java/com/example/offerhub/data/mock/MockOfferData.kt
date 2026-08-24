package com.example.offerhub.data.mock

import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.model.OfferType

object MockOfferData {
    val offers = listOf(
        Offer(
            offerId = "f1a2",
            campaignNo = "CMP-2026-000123",
            title = "20 GB Internet",
            description = "30 gün boyunca geçerli 20 GB ek internet paketi.",
            discountRate = 15.0,
            validUntil = "2026-09-30T23:59:59Z",
            score = 0.83,
            highlighted = true,
            status = OfferStatus.PENDING,
            type = OfferType.ADD_ON
        ),
        Offer(
            offerId = "f1a3",
            campaignNo = "CMP-2026-000124",
            title = "Advantage Tariff",
            description = "Daha yüksek internet kotası sunan avantajlı tarife.",
            discountRate = 10.0,
            validUntil = "2026-10-15T23:59:59Z",
            score = 0.76,
            highlighted = true,
            status = OfferStatus.PENDING,
            type = OfferType.TARIFF_UPGRADE
        ),
        Offer(
            offerId = "f1a4",
            campaignNo = "CMP-2026-000125",
            title = "Device Discount",
            description = "Seçili cihazlarda abonelere özel indirim.",
            discountRate = 12.5,
            validUntil = "2026-09-20T23:59:59Z",
            score = 0.68,
            status = OfferStatus.PENDING,
            type = OfferType.DEVICE_OFFER
        ),
        Offer(
            offerId = "f1a5",
            campaignNo = "CMP-2026-000126",
            title = "25 GB Internet",
            description = "30 gün boyunca geçerli 25 GB internet paketi.",
            discountRate = 20.0,
            validUntil = "2026-09-30T23:59:59Z",
            score = 0.91,
            highlighted = true,
            status = OfferStatus.ACCEPTED,
            type = OfferType.ADD_ON,
            acceptedAt = "2026-08-24T10:30:00Z",
            rating = 4
        ),
        Offer(
            offerId = "f1a6",
            campaignNo = "CMP-2026-000127",
            title = "Loyalty Gift",
            description = "Sadık abonelere özel hediye fırsatı.",
            validUntil = "2026-12-31T23:59:59Z",
            score = 0.72,
            status = OfferStatus.PENDING,
            type = OfferType.LOYALTY
        )
    )
}

