package com.example.offerhub.data.model

import com.google.gson.annotations.SerializedName

data class Offer(
    val offerId: String,
    val campaignNo: String,
    val title: String,
    val description: String = "",
    val discountRate: Double? = null,
    val validUntil: String? = null,
    val score: Double,
    val highlighted: Boolean = false,
    val status: OfferStatus,
    val type: OfferType,
    val acceptedAt: String? = null,
    val rating: Int? = null
)

enum class OfferStatus{
    PENDING,
    ACCEPTED,
    DECLINED
}
enum class OfferType {

    @SerializedName("EK_PAKET")
    ADD_ON,

    @SerializedName("TARIFE_YUKSELTME")
    TARIFF_UPGRADE,

    @SerializedName("CIHAZ_FIRSATI")
    DEVICE_OFFER,

    @SerializedName("SADAKAT")
    LOYALTY
}

