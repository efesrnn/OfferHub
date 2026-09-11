package com.example.offerhub.repository

import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.remote.SubscriberApi
import com.example.offerhub.data.remote.dto.RateOfferRequest
import com.example.offerhub.data.remote.dto.toDomain
import com.google.gson.Gson
import retrofit2.Response
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
            SubscriberResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        SubscriberResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        SubscriberResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    override suspend fun getOfferDetail(offerId: String): SubscriberResult<Offer> =
        offerCall { api.getOfferDetail(offerId) }

    override suspend fun acceptOffer(offerId: String): SubscriberResult<Offer> =
        actionCall { api.acceptOffer(offerId) }

    override suspend fun declineOffer(offerId: String): SubscriberResult<Offer> =
        actionCall { api.declineOffer(offerId) }

    override suspend fun rateOffer(
        offerId: String,
        rating: Int
    ): SubscriberResult<Offer> = actionCall {
        api.rateOffer(offerId, RateOfferRequest(rating))
    }

    private suspend fun offerCall(
        block: suspend () -> Response<com.example.offerhub.data.network.ApiResponse<com.example.offerhub.data.remote.dto.OfferDto>>
    ): SubscriberResult<Offer> = try {
        val response = block()
        val envelope = response.body()
        val offer = envelope?.data?.toDomain()
        if (response.isSuccessful && envelope?.success == true && offer != null) {
            SubscriberResult.Success(offer)
        } else {
            SubscriberResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        SubscriberResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        SubscriberResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    private suspend fun actionCall(
        block: suspend () -> Response<com.example.offerhub.data.network.ApiResponse<com.example.offerhub.data.remote.dto.OfferActionResponse>>
    ): SubscriberResult<Offer> = try {
        val response = block()
        val envelope = response.body()
        val offer = envelope?.data?.offer?.toDomain()
        if (response.isSuccessful && envelope?.success == true && offer != null) {
            SubscriberResult.Success(offer)
        } else {
            SubscriberResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        SubscriberResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        SubscriberResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    private fun errorFrom(response: Response<*>, bodyError: ApiError?): ApiError {
        if (bodyError != null) return bodyError
        return runCatching {
            Gson().fromJson(response.errorBody()?.string(), ErrorEnvelope::class.java).error
        }.getOrNull() ?: ApiError("UNKNOWN_ERROR")
    }

    private data class ErrorEnvelope(val error: ApiError?)
}



