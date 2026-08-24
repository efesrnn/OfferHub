package com.example.offerhub.data.local

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long
) {
    fun isAccessTokenExpired(nowEpochSeconds: Long): Boolean =
        nowEpochSeconds >= expiresAtEpochSeconds
}

interface TokenStorage {
    suspend fun save(tokens: StoredTokens)
    suspend fun read(): StoredTokens?
    suspend fun clear()
}
