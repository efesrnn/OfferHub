package com.example.offerhub.repository

import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.network.ApiError

sealed interface SubscriberResult<out T> {
    data class Success<T>(val value: T) : SubscriberResult<T>
    data class Failure(val error: ApiError) : SubscriberResult<Nothing>
}

interface SubscriberRepository {
    suspend fun getOffers(): SubscriberResult<List<Offer>>
    suspend fun getOfferDetail(offerId: String): SubscriberResult<Offer>
    suspend fun acceptOffer(offerId: String): SubscriberResult<Offer>
    suspend fun declineOffer(offerId: String): SubscriberResult<Offer>
    suspend fun rateOffer(offerId: String, rating: Int): SubscriberResult<Offer>
}
