package com.example.offerhub

import android.app.Application
import com.example.offerhub.data.local.KeystoreTokenStorage
import com.example.offerhub.data.mock.MockOfferData
import com.example.offerhub.data.remote.ApiClient
import com.example.offerhub.repository.AuthRepository
import com.example.offerhub.repository.MockSubscriberRepository

class OfferHubApplication : Application() {
    val authRepository: AuthRepository by lazy {
        AuthRepository(
            ApiClient.authApi,
            KeystoreTokenStorage(applicationContext)
        )
    }

    val subscriberRepository by lazy {
        MockSubscriberRepository(MockOfferData.offers)
    }
}
