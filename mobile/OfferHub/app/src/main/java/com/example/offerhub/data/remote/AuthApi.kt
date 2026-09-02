package com.example.offerhub.data.remote

import com.example.offerhub.data.model.auth.AuthData
import com.example.offerhub.data.model.auth.ChangePasswordRequest
import com.example.offerhub.data.model.auth.OtpVerifyRequest
import com.example.offerhub.data.model.auth.OtpRequestData
import com.example.offerhub.data.model.auth.OtpRequestRequest
import com.example.offerhub.data.model.auth.StaffLoginRequest
import com.example.offerhub.data.model.auth.SubscriberRegisterData
import com.example.offerhub.data.model.auth.SubscriberRegisterRequest
import com.example.offerhub.data.network.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun registerSubscriber(
        @Body request: SubscriberRegisterRequest
    ): Response<ApiResponse<SubscriberRegisterData>>

    @POST("api/v1/auth/otp-request")
    suspend fun requestOtp(
        @Body request: OtpRequestRequest
    ): Response<ApiResponse<OtpRequestData>>

    @POST("api/v1/auth/otp-verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<ApiResponse<AuthData>>

    @POST("api/v1/auth/login")
    suspend fun staffLogin(@Body request: StaffLoginRequest): Response<ApiResponse<AuthData>>

    @POST("api/v1/auth/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<Unit>>
}
