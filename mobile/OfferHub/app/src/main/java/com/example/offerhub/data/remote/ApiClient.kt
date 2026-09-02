package com.example.offerhub.data.remote

import com.example.offerhub.BuildConfig
import com.example.offerhub.data.local.AccessTokenProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
        }

    private fun createRetrofit(client: OkHttpClient) =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

    fun createAuthApi(tokenProvider: AccessTokenProvider): AuthApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .build()
        return createRetrofit(client).create(AuthApi::class.java)
    }

    fun createSubscriberApi(tokenProvider: AccessTokenProvider): SubscriberApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .build()
        return createRetrofit(client).create(SubscriberApi::class.java)
    }

    fun createExpertApi(tokenProvider: AccessTokenProvider): ExpertApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .build()
        return createRetrofit(client).create(ExpertApi::class.java)
    }

    fun createGamificationApi(tokenProvider: AccessTokenProvider): GamificationApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .build()
        return createRetrofit(client).create(GamificationApi::class.java)
    }
}
