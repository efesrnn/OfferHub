package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.model.OfferType

data class OfferDto(
    val offerId: String,
    val campaignNo: String,
    val title: String,
    val description: String? = null,
    val discountRate: Double? = null,
    val validUntil: String? = null,
    val score: Double,
    val highlighted: Boolean = false,
    val status: String,
    val type: String,
    val acceptedAt: String? = null,
    val rating: Int? = null
)

fun OfferDto.toDomain(): Offer? {
    val offerStatus = runCatching { OfferStatus.valueOf(status) }.getOrNull()
        ?: return null
    val offerType = when (type) {
        "EK_PAKET" -> OfferType.ADD_ON
        "TARIFE_YUKSELTME" -> OfferType.TARIFF_UPGRADE
        "CIHAZ_FIRSATI" -> OfferType.DEVICE_OFFER
        "SADAKAT" -> OfferType.LOYALTY
        else -> return null
    }

    return Offer(
        offerId = offerId,
        campaignNo = campaignNo,
        title = title,
        description = description.orEmpty(),
        discountRate = discountRate,
        validUntil = validUntil,
        score = score,
        highlighted = highlighted,
        status = offerStatus,
        type = offerType,
        acceptedAt = acceptedAt,
        rating = rating
    )
}

