package com.example.offerhub

import android.app.Application
import com.example.offerhub.data.local.KeystoreTokenStorage
import com.example.offerhub.data.local.SessionTokenProvider
import com.example.offerhub.data.mock.MockOfferData
import com.example.offerhub.data.remote.ApiClient
import com.example.offerhub.repository.AuthRepository
import com.example.offerhub.repository.MockSubscriberRepository
import com.example.offerhub.repository.SubscriberRepositoryImpl
import com.example.offerhub.repository.MockAdminRepository
import com.example.offerhub.repository.MockExpertRepository
import com.example.offerhub.repository.MockGamificationRepository
import com.example.offerhub.repository.MockSupervisorRepository

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

    val adminRepository by lazy {
        MockAdminRepository()
    }

    val expertRepository by lazy {
        MockExpertRepository()
    }

    val gamificationRepository by lazy {
        MockGamificationRepository()
    }

    val supervisorRepository by lazy {
        MockSupervisorRepository()
    }
}
