package com.example.offerhub

import android.app.Application
import com.example.offerhub.data.local.KeystoreTokenStorage
import com.example.offerhub.data.local.SessionTokenProvider
import com.example.offerhub.data.mock.MockOfferData
import com.example.offerhub.data.remote.ApiClient
import com.example.offerhub.repository.AuthRepository
import com.example.offerhub.repository.MockSubscriberRepository
import com.example.offerhub.repository.SubscriberRepositoryImpl

class OfferHubApplication : Application() {
    private val sessionTokenProvider = SessionTokenProvider()
    private val tokenStorage by lazy {
        KeystoreTokenStorage(applicationContext, sessionTokenProvider)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            ApiClient.createAuthApi(),
            tokenStorage
        )
    }

    val realSubscriberRepository by lazy {
        SubscriberRepositoryImpl(
            ApiClient.createSubscriberApi(sessionTokenProvider)
        )
    }

    val subscriberRepository by lazy {
        MockSubscriberRepository(MockOfferData.offers)
    }
}
