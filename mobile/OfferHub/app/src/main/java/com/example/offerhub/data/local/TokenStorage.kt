package com.example.offerhub.data.local

data class StoredTokens(val accessToken: String, val refreshToken: String, val expiresIn: Long)

interface TokenStorage {
    suspend fun save(tokens: StoredTokens)
    suspend fun read(): StoredTokens?
    suspend fun clear()
}
