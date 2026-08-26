package com.example.offerhub.data.remote

import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.remote.dto.OfferActionResponse
import com.example.offerhub.data.remote.dto.OfferDto
import com.example.offerhub.data.remote.dto.RateOfferRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SubscriberApi {
    @GET("api/v1/subscribers/me/offers")
    suspend fun getOffers(): Response<ApiResponse<List<OfferDto>>>

    @GET("api/v1/subscribers/me/offers/{offerId}")
    suspend fun getOfferDetail(
        @Path("offerId") offerId: String
    ): Response<ApiResponse<OfferDto>>

    @POST("api/v1/subscribers/me/offers/{offerId}/accept")
    suspend fun acceptOffer(
        @Path("offerId") offerId: String
    ): Response<ApiResponse<OfferActionResponse>>

    @POST("api/v1/subscribers/me/offers/{offerId}/decline")
    suspend fun declineOffer(
        @Path("offerId") offerId: String
    ): Response<ApiResponse<OfferActionResponse>>

    @POST("api/v1/subscribers/me/offers/{offerId}/rating")
    suspend fun rateOffer(
        @Path("offerId") offerId: String,
        @Body request: RateOfferRequest
    ): Response<ApiResponse<OfferActionResponse>>
}

