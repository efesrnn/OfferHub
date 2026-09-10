package com.example.offerhub.data.remote

import com.example.offerhub.BuildConfig
import com.example.offerhub.data.local.AccessTokenProvider
import okhttp3.Authenticator
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

    /**
     * No authenticator on this one, deliberately: TokenAuthenticator's refresh call goes
     * through this exact client (see AuthRepository/OfferHubApplication), so if this client
     * authenticated itself, a failing /refresh would try to refresh its way out of failing.
     */
    fun createAuthApi(tokenProvider: AccessTokenProvider): AuthApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .build()
        return createRetrofit(client).create(AuthApi::class.java)
    }

    fun createAdminApi(tokenProvider: AccessTokenProvider, authenticator: Authenticator): AdminApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .build()
        return createRetrofit(client).create(AdminApi::class.java)
    }

    fun createSubscriberApi(tokenProvider: AccessTokenProvider, authenticator: Authenticator): SubscriberApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .build()
        return createRetrofit(client).create(SubscriberApi::class.java)
    }

    fun createExpertApi(tokenProvider: AccessTokenProvider, authenticator: Authenticator): ExpertApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .build()
        return createRetrofit(client).create(ExpertApi::class.java)
    }

    fun createGamificationApi(tokenProvider: AccessTokenProvider, authenticator: Authenticator): GamificationApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .build()
        return createRetrofit(client).create(GamificationApi::class.java)
    }

    fun createSupervisorApi(tokenProvider: AccessTokenProvider, authenticator: Authenticator): SupervisorApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .build()
        return createRetrofit(client).create(SupervisorApi::class.java)
    }
}
