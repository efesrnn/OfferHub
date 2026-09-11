package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.SubscriberInsight

data class SubscriberInsightDto(
    val subscriberId: String,
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

fun SubscriberInsightDto.toDomain(): SubscriberInsight = SubscriberInsight(
    segment = segment,
    reason = reason,
    tenureMonths = tenureMonths,
    monthlyDataUsageGb = monthlyDataUsageGb,
    monthlySpendTry = monthlySpendTry,
    complaintCount6m = complaintCount6m,
    usageTrend = usageTrend,
    pastAcceptedOffers = pastAcceptedOffers,
    pastDeclinedOffers = pastDeclinedOffers,
    currentTariff = currentTariff
)
