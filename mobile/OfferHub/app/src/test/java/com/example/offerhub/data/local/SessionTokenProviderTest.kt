package com.example.offerhub.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTokenProviderTest {
    @Test
    fun `access token can be updated and cleared`() {
        val provider = SessionTokenProvider()

        assertNull(provider.accessToken())

        provider.update("access-token")
        assertEquals("access-token", provider.accessToken())

        provider.clear()
        assertNull(provider.accessToken())
    }
}
