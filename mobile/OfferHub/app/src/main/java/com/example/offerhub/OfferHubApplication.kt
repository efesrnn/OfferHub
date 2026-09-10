package com.example.offerhub

import android.app.Application
import com.example.offerhub.data.local.KeystoreTokenStorage
import com.example.offerhub.data.local.SessionEvents
import com.example.offerhub.data.local.SessionTokenProvider
import com.example.offerhub.data.mock.MockOfferData
import com.example.offerhub.data.remote.ApiClient
import com.example.offerhub.data.remote.TokenAuthenticator
import com.example.offerhub.repository.AuthRepository
import com.example.offerhub.repository.MockSubscriberRepository
import com.example.offerhub.repository.SubscriberRepositoryImpl
import com.example.offerhub.repository.AdminRepository
import com.example.offerhub.repository.AdminRepositoryImpl
import com.example.offerhub.repository.MockAdminRepository
import com.example.offerhub.repository.MockExpertRepository
import com.example.offerhub.repository.ExpertRepository
import com.example.offerhub.repository.ExpertRepositoryImpl
import com.example.offerhub.repository.MockGamificationRepository
import com.example.offerhub.repository.GamificationRepositoryImpl
import com.example.offerhub.repository.GamificationRepository
import com.example.offerhub.repository.MockSupervisorRepository
import com.example.offerhub.repository.SupervisorRepository
import com.example.offerhub.repository.SupervisorRepositoryImpl

class OfferHubApplication : Application() {
    private val sessionTokenProvider = SessionTokenProvider()
    private val tokenStorage by lazy {
        KeystoreTokenStorage(applicationContext, sessionTokenProvider)
    }

    /** Fires when a silent token refresh fails, so the UI can fall back to a real logout. */
    val sessionEvents = SessionEvents()

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            ApiClient.createAuthApi(sessionTokenProvider),
            tokenStorage
        )
    }

    /**
     * Shared by every API client below except AuthApi's own (see ApiClient.createAuthApi for
     * why). One instance app-wide so its mutex actually serializes refresh attempts across
     * every screen instead of each client racing its own copy.
     */
    private val tokenAuthenticator by lazy {
        TokenAuthenticator(authRepository, sessionTokenProvider, sessionEvents)
    }

    val realSubscriberRepository by lazy {
        SubscriberRepositoryImpl(
            ApiClient.createSubscriberApi(sessionTokenProvider, tokenAuthenticator)
        )
    }

    val subscriberRepository by lazy {
        if (BuildConfig.USE_MOCK_SUBSCRIBER) {
            MockSubscriberRepository(MockOfferData.offers)
        } else {
            realSubscriberRepository
        }
    }

    val adminRepository: AdminRepository by lazy {
        if (BuildConfig.USE_MOCK_ADMIN) {
            MockAdminRepository()
        } else {
            AdminRepositoryImpl(
                ApiClient.createAdminApi(sessionTokenProvider, tokenAuthenticator)
            )
        }
    }

    val expertRepository: ExpertRepository by lazy {
        if (BuildConfig.USE_MOCK_EXPERT) {
            MockExpertRepository()
        } else {
            ExpertRepositoryImpl(
                ApiClient.createExpertApi(sessionTokenProvider, tokenAuthenticator)
            )
        }
    }

    val gamificationRepository: GamificationRepository by lazy {
        if (BuildConfig.USE_MOCK_GAMIFICATION) {
            MockGamificationRepository()
        } else {
            GamificationRepositoryImpl(
                ApiClient.createGamificationApi(sessionTokenProvider, tokenAuthenticator)
            )
        }
    }

    val supervisorRepository: SupervisorRepository by lazy {
        if (BuildConfig.USE_MOCK_SUPERVISOR) {
            MockSupervisorRepository()
        } else {
            SupervisorRepositoryImpl(
                ApiClient.createSupervisorApi(sessionTokenProvider, tokenAuthenticator)
            )
        }
    }
}
