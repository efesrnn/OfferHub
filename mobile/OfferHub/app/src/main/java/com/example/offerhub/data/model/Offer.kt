package com.example.offerhub.data.model

data class Offer (
val offerId: String,
val campaignNo: String,
val title: String,
val score: Double,
val highlighted: Boolean,
val status: String
)
enum class OfferStatus{
    PENDING,
    ACCEPTED,
    DECLINED
}