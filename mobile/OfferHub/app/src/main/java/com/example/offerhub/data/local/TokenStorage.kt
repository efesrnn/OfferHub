package com.example.offerhub.data.local

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val userId: String,
    val userRole: String,
    val phone: String? = null,
    val passwordChangeRequired: Boolean = false
) {
    fun isAccessTokenExpired(nowEpochSeconds: Long): Boolean =
        nowEpochSeconds >= expiresAtEpochSeconds
}

interface TokenStorage {
    suspend fun save(tokens: StoredTokens)
    suspend fun read(): StoredTokens?
    suspend fun clear()
}
