package com.example.offerhub.data.model

/**
 * AI Service'in bir abone icin urettigi segment + o segmenti tetikleyen ham sinyalden
 * turetilmis okunabilir aciklama. Iki abone ayni segmentte (ornegin PASIF) olsa da
 * "reason" ve ham alanlar onlari birbirinden ayirt etmeye yarar.
 */
data class SubscriberInsight(
    val segment: String,
    val reason: String,
    val tenureMonths: Int,
    val monthlyDataUsageGb: Double,
    val monthlySpendTry: Double,
    val complaintCount6m: Int,
    val usageTrend: Double,
    val pastAcceptedOffers: Int,
    val pastDeclinedOffers: Int,
    val currentTariff: String
)
