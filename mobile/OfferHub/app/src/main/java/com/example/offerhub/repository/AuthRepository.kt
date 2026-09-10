package com.example.offerhub.repository

import com.example.offerhub.data.local.StoredTokens
import com.example.offerhub.data.local.TokenStorage
import com.example.offerhub.data.model.auth.AuthData
import com.example.offerhub.data.model.auth.AuthMode
import com.example.offerhub.data.model.auth.AuthUser
import com.example.offerhub.data.model.auth.ChangePasswordRequest
import com.example.offerhub.data.model.auth.OtpRequestData
import com.example.offerhub.data.model.auth.OtpRequestRequest
import com.example.offerhub.data.model.auth.OtpVerifyRequest
import com.example.offerhub.data.model.auth.RefreshRequest
import com.example.offerhub.data.model.auth.StaffLoginRequest
import com.example.offerhub.data.model.auth.SubscriberRegisterData
import com.example.offerhub.data.model.auth.SubscriberRegisterRequest
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.remote.AuthApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import java.io.IOException
import java.time.Instant

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val error: ApiError) : AuthResult<Nothing>
}
private data class ApiErrorEnvelope(
    val error: ApiError?
)

data class RestoredAuthSession(
    val user: AuthUser,
    val passwordChangeRequired: Boolean
)

class AuthRepository(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
    private val gson: Gson = Gson()
) {
    suspend fun registerSubscriber(firstName: String, lastName: String, phone: String, email: String?, authMode: AuthMode) =
        call { api.registerSubscriber(SubscriberRegisterRequest(firstName, lastName, phone, email, authMode)) }

    suspend fun requestOtp(phone: String, authMode: AuthMode): AuthResult<OtpRequestData> =
        call { api.requestOtp(OtpRequestRequest(authMode, phone)) }

    suspend fun verifyOtp(authMode: AuthMode, phone: String, credential: String): AuthResult<AuthData> =
        call { api.verifyOtp(OtpVerifyRequest(authMode, phone, credential)) }
            .withSubscriberPhone(phone)
            .saveTokensOnSuccess()
    suspend fun staffLogin(email: String, password: String): AuthResult<AuthData> =
        call { api.staffLogin(StaffLoginRequest(email, password)) }.saveTokensOnSuccess()

    private suspend fun AuthResult<AuthData>.saveTokensOnSuccess(): AuthResult<AuthData> {
        if (this is AuthResult.Success) {
            tokenStorage.save(
                StoredTokens(
                    accessToken = value.accessToken,
                    refreshToken = value.refreshToken,
                    expiresAtEpochSeconds =
                        Instant.now().epochSecond + value.expiresIn,
                    userId = value.user.id,
                    userRole = value.user.role,
                    phone = value.user.phone,
                    passwordChangeRequired = value.passwordChangeRequired
                )
            )
        }
        return this
    }

    private fun AuthResult<AuthData>.withSubscriberPhone(phone: String): AuthResult<AuthData> =
        when (this) {
            is AuthResult.Success -> AuthResult.Success(
                value.copy(user = value.user.copy(phone = phone))
            )
            is AuthResult.Failure -> this
        }

    /**
     * The refresh response never carries a phone (identity's AuthUserResponse has no such
     * field, only login/otp-verify get one). Without this, a subscriber's phone number would
     * silently disappear from the session the first time their access token refreshes.
     */
    private fun AuthResult<AuthData>.withPreservedPhone(phone: String?): AuthResult<AuthData> =
        when {
            this is AuthResult.Success && phone != null ->
                AuthResult.Success(value.copy(user = value.user.copy(phone = phone)))
            else -> this
        }

    suspend fun clearLocalSession() {
        tokenStorage.clear()
    }

    /**
     * Spends the stored refresh token for a new access/refresh pair. Called by
     * TokenAuthenticator when a request comes back 401, never directly by a screen - this is
     * what makes the access token's 15-minute expiry invisible to the rest of the app.
     *
     * On failure the local session is cleared: a rejected refresh token means the session is
     * over server-side (expired, or logged out from elsewhere), so holding onto the old
     * tokens would only produce more 401s.
     */
    suspend fun refresh(): AuthResult<AuthData> {
        val stored = tokenStorage.read() ?: return AuthResult.Failure(ApiError("SESSION_EXPIRED"))

        val result = call { api.refresh(RefreshRequest(stored.refreshToken)) }
            .withPreservedPhone(stored.phone)
            .saveTokensOnSuccess()

        if (result is AuthResult.Failure) {
            tokenStorage.clear()
        }
        return result
    }

    /**
     * Best-effort: tells the server to revoke the refresh token so it cannot be replayed,
     * but the local session is cleared either way. A logout that "fails" because the network
     * is down should still log the user out on this device.
     */
    suspend fun logout() {
        tokenStorage.read()?.let { stored ->
            runCatching { api.logout(RefreshRequest(stored.refreshToken)) }
        }
        tokenStorage.clear()
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): AuthResult<Unit> = try {
        val response = api.changePassword(
            ChangePasswordRequest(currentPassword, newPassword)
        )
        val envelope = response.body()
        if (response.isSuccessful && envelope?.success == true) {
            tokenStorage.clear()
            AuthResult.Success(Unit)
        } else {
            AuthResult.Failure(
                envelope?.error ?: parseError(response) ?: ApiError("UNKNOWN_ERROR")
            )
        }
    } catch (_: IOException) {
        AuthResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        AuthResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    suspend fun restoreLocalSession(): RestoredAuthSession? {
        val tokens = tokenStorage.read() ?: return null
        if (tokens.isAccessTokenExpired(Instant.now().epochSecond)) {
            tokenStorage.clear()
            return null
        }
        if (tokens.passwordChangeRequired) {
            tokenStorage.clear()
            return null
        }
        return RestoredAuthSession(
            user = AuthUser(
                id = tokens.userId,
                role = tokens.userRole,
                phone = tokens.phone
            ),
            passwordChangeRequired = tokens.passwordChangeRequired
        )
    }

    private suspend fun <T> call(block: suspend () -> Response<ApiResponse<T>>): AuthResult<T> = try {
        val response = block()
        val envelope = response.body()
        if (response.isSuccessful && envelope?.success == true && envelope.data != null) {
            AuthResult.Success(envelope.data)
        } else {
            AuthResult.Failure(envelope?.error ?: parseError(response) ?: ApiError("UNKNOWN_ERROR"))
        }
    } catch (_: IOException) {
        AuthResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        AuthResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    private fun <T> parseError(
        response: Response<ApiResponse<T>>
    ): ApiError? {
        val body = response.errorBody()?.string()
            ?: return null

        return runCatching {
            gson.fromJson(
                body,
                ApiErrorEnvelope::class.java
            ).error
        }.getOrNull()
    }
}
