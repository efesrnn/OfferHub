package com.example.offerhub.data.remote

import com.example.offerhub.data.local.AccessTokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class AuthorizationInterceptor(
    private val tokenProvider: AccessTokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenProvider.accessToken()

        if (accessToken.isNullOrBlank() || originalRequest.header(AUTHORIZATION) != null) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header(AUTHORIZATION, "Bearer $accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
