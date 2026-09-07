package com.example.offerhub.viewModel

import com.example.offerhub.R
import com.example.offerhub.data.local.StoredTokens
import com.example.offerhub.data.local.TokenStorage
import com.example.offerhub.data.model.auth.AuthData
import com.example.offerhub.data.model.auth.ChangePasswordRequest
import com.example.offerhub.data.model.auth.OtpRequestData
import com.example.offerhub.data.model.auth.OtpRequestRequest
import com.example.offerhub.data.model.auth.OtpVerifyRequest
import com.example.offerhub.data.model.auth.StaffLoginRequest
import com.example.offerhub.data.model.auth.SubscriberRegisterData
import com.example.offerhub.data.model.auth.SubscriberRegisterRequest
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.remote.AuthApi
import com.example.offerhub.repository.AuthRepository
import com.example.offerhub.testutil.MainDispatcherRule
import com.example.offerhub.ui.text.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `valid local session is restored when view model starts`() = runTest {
        val tokenStorage = FakeTokenStorage(
            initialTokens = validTokens()
        )
        val viewModel = AuthViewModel(
            repository = AuthRepository(
                api = NoOpAuthApi(),
                tokenStorage = tokenStorage
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isSessionChecking)
        assertEquals("expert-1", state.currentUser?.id)
        assertEquals("EXPERT", state.currentUser?.role)
        assertEquals("EXPERT", state.pendingNavigationRole)
    }

    @Test
    fun `logout clears session before completing`() = runTest {
        val tokenStorage = FakeTokenStorage(
            initialTokens = validTokens()
        )
        val viewModel = AuthViewModel(
            repository = AuthRepository(
                api = NoOpAuthApi(),
                tokenStorage = tokenStorage
            )
        )

        advanceUntilIdle()

        var logoutCompleted = false

        viewModel.logout {
            assertNull(tokenStorage.storedTokens)
            assertNull(viewModel.uiState.value.currentUser)
            logoutCompleted = true
        }

        advanceUntilIdle()

        assertTrue(logoutCompleted)
        assertNull(tokenStorage.storedTokens)
        assertNull(viewModel.uiState.value.currentUser)
        assertFalse(viewModel.uiState.value.isSessionChecking)
    }

    private fun validTokens() = StoredTokens(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAtEpochSeconds = Long.MAX_VALUE,
        userId = "expert-1",
        userRole = "EXPERT"
    )

    private class FakeTokenStorage(
        initialTokens: StoredTokens? = null
    ) : TokenStorage {

        var storedTokens: StoredTokens? = initialTokens
            private set

        override suspend fun save(tokens: StoredTokens) {
            storedTokens = tokens
        }

        override suspend fun read(): StoredTokens? =
            storedTokens

        override suspend fun clear() {
            storedTokens = null
        }
    }

    private class NoOpAuthApi(
        private val otpResponse:
        Response<ApiResponse<OtpRequestData>>? = null
    ) : AuthApi {
        override suspend fun registerSubscriber(
            request: SubscriberRegisterRequest
        ): Response<ApiResponse<SubscriberRegisterData>> {
            error("Not required for this test")
        }

        override suspend fun requestOtp(
            request: OtpRequestRequest
        ): Response<ApiResponse<OtpRequestData>> {
            return requireNotNull(otpResponse) {
                "OTP response was not configured for this test"
            }
        }

        override suspend fun verifyOtp(
            request: OtpVerifyRequest
        ): Response<ApiResponse<AuthData>> {
            error("Not required for this test")
        }

        override suspend fun staffLogin(
            request: StaffLoginRequest
        ): Response<ApiResponse<AuthData>> {
            error("Not required for this test")
        }

        override suspend fun changePassword(
            request: ChangePasswordRequest
        ): Response<ApiResponse<Unit>> {
            error("Not required for this test")
        }
    }
    @Test
    fun `failed otp delivery shows error without opening verification`() =
        runTest {
            val api = NoOpAuthApi(
                otpResponse = Response.success(
                    ApiResponse(
                        success = true,
                        data = OtpRequestData(
                            otpSent = false
                        ),
                        error = null
                    )
                )
            )
            val viewModel = AuthViewModel(
                repository = AuthRepository(
                    api = api,
                    tokenStorage = FakeTokenStorage()
                )
            )

            runCurrent()

            viewModel.requestOtpForLogin(
                phone = "5551234567"
            )

            runCurrent()

            val state = viewModel.uiState.value

            assertFalse(state.otpReady)
            assertFalse(state.isOtpRequestLoading)
            assertEquals(
                0,
                state.resendCooldownSeconds
            )

            val errorMessage =
                state.errorMessage as UiText.Resource

            assertEquals(
                R.string.error_otp_send_failed,
                errorMessage.resourceId
            )
        }
    @Test
    fun `successful otp request opens verification and starts resend cooldown`() =
        runTest {
            val tokenStorage = FakeTokenStorage()
            val api = NoOpAuthApi(
                otpResponse = Response.success(
                    ApiResponse(
                        success = true,
                        data = OtpRequestData(
                            otpSent = true
                        ),
                        error = null
                    )
                )
            )
            val viewModel = AuthViewModel(
                repository = AuthRepository(
                    api = api,
                    tokenStorage = tokenStorage
                )
            )

            runCurrent()

            viewModel.requestOtpForLogin(
                phone = "5551234567"
            )

            runCurrent()

            val initialState = viewModel.uiState.value

            assertEquals(
                "5551234567",
                initialState.pendingPhone
            )
            assertTrue(initialState.otpReady)
            assertFalse(initialState.isOtpRequestLoading)
            assertEquals(
                30,
                initialState.resendCooldownSeconds
            )

            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(
                29,
                viewModel.uiState.value.resendCooldownSeconds
            )
        }
}