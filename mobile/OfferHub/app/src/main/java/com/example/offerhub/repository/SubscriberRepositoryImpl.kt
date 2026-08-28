package com.example.offerhub.repository

import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.remote.SubscriberApi
import com.example.offerhub.data.remote.dto.RateOfferRequest
import com.example.offerhub.data.remote.dto.toDomain
import java.io.IOException

class SubscriberRepositoryImpl(
    private val api: SubscriberApi
) : SubscriberRepository {
    override suspend fun getOffers(): SubscriberResult<List<Offer>> = try {
        val response = api.getOffers()
        val envelope = response.body()
        val offers = envelope?.data
            ?.mapNotNull { it.toDomain() }
            ?.sortedByDescending { it.score }

        if (response.isSuccessful && envelope?.success == true && offers != null) {
            SubscriberResult.Success(offers)
        } else {
            SubscriberResult.Failure(envelope?.error ?: ApiError("UNKNOWN_ERROR"))
        }
    } catch (_: IOException) {
        SubscriberResult.Failure(ApiError("NETWORK_ERROR"))
    }

    override suspend fun getOfferDetail(offerId: String): SubscriberResult<Offer> =
        offerCall { api.getOfferDetail(offerId).body() }

    override suspend fun acceptOffer(offerId: String): SubscriberResult<Offer> =
        actionCall { api.acceptOffer(offerId).body() }

    override suspend fun declineOffer(offerId: String): SubscriberResult<Offer> =
        actionCall { api.declineOffer(offerId).body() }

    override suspend fun rateOffer(
        offerId: String,
        rating: Int
    ): SubscriberResult<Offer> = actionCall {
        api.rateOffer(offerId, RateOfferRequest(rating)).body()
    }

    private suspend fun offerCall(
        block: suspend () -> com.example.offerhub.data.network.ApiResponse<com.example.offerhub.data.remote.dto.OfferDto>?
    ): SubscriberResult<Offer> = try {
        val envelope = block()
        val offer = envelope?.data?.toDomain()
        if (envelope?.success == true && offer != null) {
            SubscriberResult.Success(offer)
        } else {
            SubscriberResult.Failure(envelope?.error ?: ApiError("UNKNOWN_ERROR"))
        }
    } catch (_: IOException) {
        SubscriberResult.Failure(ApiError("NETWORK_ERROR"))
    }

    private suspend fun actionCall(
        block: suspend () -> com.example.offerhub.data.network.ApiResponse<com.example.offerhub.data.remote.dto.OfferActionResponse>?
    ): SubscriberResult<Offer> = try {
        val envelope = block()
        val offer = envelope?.data?.offer?.toDomain()
        if (envelope?.success == true && offer != null) {
            SubscriberResult.Success(offer)
        } else {
            SubscriberResult.Failure(envelope?.error ?: ApiError("UNKNOWN_ERROR"))
        }
    } catch (_: IOException) {
        SubscriberResult.Failure(ApiError("NETWORK_ERROR"))
    }
}



