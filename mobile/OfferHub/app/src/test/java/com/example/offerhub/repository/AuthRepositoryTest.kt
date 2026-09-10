package com.example.offerhub.repository

import com.example.offerhub.data.local.StoredTokens
import com.example.offerhub.data.local.TokenStorage
import com.example.offerhub.data.model.auth.AuthData
import com.example.offerhub.data.model.auth.AuthUser
import com.example.offerhub.data.model.auth.ChangePasswordRequest
import com.example.offerhub.data.model.auth.OtpRequestData
import com.example.offerhub.data.model.auth.OtpRequestRequest
import com.example.offerhub.data.model.auth.OtpVerifyRequest
import com.example.offerhub.data.model.auth.StaffLoginRequest
import com.example.offerhub.data.model.auth.SubscriberRegisterData
import com.example.offerhub.data.model.auth.SubscriberRegisterRequest
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.remote.AuthApi
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AuthRepositoryTest {

    @Test
    fun `successful staff login saves authentication tokens`() = runBlocking {
        val authData = AuthData(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 3600,
            user = AuthUser(
                id = "expert-1",
                role = "EXPERT"
            )
        )

        val api = FakeAuthApi(
            loginResponse = Response.success(
                ApiResponse(
                    success = true,
                    data = authData,
                    error = null
                )
            )
        )
        val tokenStorage = FakeTokenStorage()
        val repository = AuthRepository(
            api = api,
            tokenStorage = tokenStorage
        )

        val result = repository.staffLogin(
            email = "expert@offerhub.com",
            password = "Password1!"
        )

        assertTrue(result is AuthResult.Success)

        val storedTokens = tokenStorage.savedTokens
        assertNotNull(storedTokens)
        assertEquals("access-token", storedTokens?.accessToken)
        assertEquals("refresh-token", storedTokens?.refreshToken)
        assertEquals("expert-1", storedTokens?.userId)
        assertEquals("EXPERT", storedTokens?.userRole)
        assertTrue(
            requireNotNull(storedTokens).expiresAtEpochSeconds > 0
        )
    }

    @Test
    fun `expired local session is cleared and not restored`() = runBlocking {
        val tokenStorage = FakeTokenStorage().apply {
            savedTokens = StoredTokens(
                accessToken = "expired-access-token",
                refreshToken = "refresh-token",
                expiresAtEpochSeconds = 0,
                userId = "expert-1",
                userRole = "EXPERT"
            )
        }

        val repository = AuthRepository(
            api = FakeAuthApi(
                loginResponse = Response.success(
                    ApiResponse<AuthData>(
                        success = false,
                        data = null,
                        error = null
                    )
                )
            ),
            tokenStorage = tokenStorage
        )

        val restoredSession = repository.restoreLocalSession()

        assertNull(restoredSession)
        assertNull(tokenStorage.savedTokens)
    }

    @Test
    fun `staff login preserves backend error and does not save tokens`() = runBlocking {
        val errorJson = """
        {
          "success": false,
          "data": null,
          "error": {
            "code": "INVALID_CREDENTIALS",
            "message": "Invalid email or password"
          }
        }
    """.trimIndent()

        val api = FakeAuthApi(
            loginResponse = Response.error(
                401,
                errorJson.toResponseBody(
                    "application/json".toMediaType()
                )
            )
        )
        val tokenStorage = FakeTokenStorage()
        val repository = AuthRepository(
            api = api,
            tokenStorage = tokenStorage
        )

        val result = repository.staffLogin(
            email = "expert@offerhub.com",
            password = "wrong-password"
        )

        assertTrue(result is AuthResult.Failure)
        assertEquals(
            "INVALID_CREDENTIALS",
            (result as AuthResult.Failure).error.code
        )
        assertNull(tokenStorage.savedTokens)
    }

    private class FakeTokenStorage : TokenStorage {
        var savedTokens: StoredTokens? = null

        override suspend fun save(tokens: StoredTokens) {
            savedTokens = tokens
        }

        override suspend fun read(): StoredTokens? = savedTokens

        override suspend fun clear() {
            savedTokens = null
        }
    }

    private class FakeAuthApi(
        private val loginResponse: Response<ApiResponse<AuthData>>,
        private val loginException: IOException? = null
    ) : AuthApi {

        override suspend fun staffLogin(
            request: StaffLoginRequest
        ): Response<ApiResponse<AuthData>> {
            loginException?.let { throw it }
            return loginResponse
        }

        override suspend fun registerSubscriber(
            request: SubscriberRegisterRequest
        ): Response<ApiResponse<SubscriberRegisterData>> {
            error("Not required for this test")
        }

        override suspend fun requestOtp(
            request: OtpRequestRequest
        ): Response<ApiResponse<OtpRequestData>> {
            error("Not required for this test")
        }

        override suspend fun verifyOtp(
            request: OtpVerifyRequest
        ): Response<ApiResponse<AuthData>> {
            error("Not required for this test")
        }

        override suspend fun changePassword(
            request: ChangePasswordRequest
        ): Response<ApiResponse<Unit>> {
            error("Not required for this test")
        }

        override suspend fun refresh(
            request: com.example.offerhub.data.model.auth.RefreshRequest
        ): Response<ApiResponse<AuthData>> {
            error("Not required for this test")
        }

        override suspend fun logout(
            request: com.example.offerhub.data.model.auth.RefreshRequest
        ): Response<ApiResponse<Unit>> {
            error("Not required for this test")
        }
    }
    @Test
    fun `staff login returns network error when request fails`() = runBlocking {
        val api = FakeAuthApi(
            loginResponse = Response.success(
                ApiResponse<AuthData>(
                    success = false,
                    data = null,
                    error = null
                )
            ),
            loginException = IOException("Connection failed")
        )
        val tokenStorage = FakeTokenStorage()
        val repository = AuthRepository(
            api = api,
            tokenStorage = tokenStorage
        )

        val result = repository.staffLogin(
            email = "expert@offerhub.com",
            password = "Password1!"
        )

        assertTrue(result is AuthResult.Failure)
        assertEquals(
            "NETWORK_ERROR",
            (result as AuthResult.Failure).error.code
        )
        assertNull(tokenStorage.savedTokens)
    }
}