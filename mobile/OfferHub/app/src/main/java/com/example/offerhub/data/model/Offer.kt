package com.example.offerhub.data.model

import com.google.gson.annotations.SerializedName

data class Offer(
    val offerId: String,
    val campaignNo: String,
    val title: String,
    val score: Double,
    val highlighted: Boolean,
    val status: String,
    val type: OfferType
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

