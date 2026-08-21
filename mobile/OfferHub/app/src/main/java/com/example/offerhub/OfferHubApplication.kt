package com.example.offerhub

import android.app.Application
import com.example.offerhub.data.local.KeystoreTokenStorage
import com.example.offerhub.data.remote.ApiClient
import com.example.offerhub.repository.AuthRepository

class OfferHubApplication : Application() {
    val authRepository: AuthRepository by lazy {
        AuthRepository(ApiClient.createAuthApi(), KeystoreTokenStorage(applicationContext))
    }
}

//API,token storage and repository created for one time
