package com.example.offerhub.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredTokensTest {
    private val tokens = StoredTokens(
        accessToken = "access",
        refreshToken = "refresh",
        expiresAtEpochSeconds = 1_000L,
        userId = "user-1",
        userRole = "SUBSCRIBER"
    )

    @Test
    fun `token is valid before expiration time`() {
        assertFalse(tokens.isAccessTokenExpired(999L))
    }

    @Test
    fun `token is expired at expiration time`() {
        assertTrue(tokens.isAccessTokenExpired(1_000L))
    }
}
