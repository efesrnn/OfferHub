package com.example.offerhub.data.local

interface AccessTokenProvider {
    fun accessToken(): String?
}

class SessionTokenProvider : AccessTokenProvider {
    @Volatile
    private var currentAccessToken: String? = null

    override fun accessToken(): String? = currentAccessToken

    fun update(accessToken: String) {
        currentAccessToken = accessToken
    }

    fun clear() {
        currentAccessToken = null
    }
}
