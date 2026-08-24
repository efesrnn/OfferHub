package com.example.offerhub.repository

import com.example.offerhub.data.local.StoredTokens
import com.example.offerhub.data.local.TokenStorage
import com.example.offerhub.data.model.auth.AuthData
import com.example.offerhub.data.model.auth.AuthUser
import com.example.offerhub.data.model.auth.OtpVerifyRequest
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

class AuthRepository(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
    private val gson: Gson = Gson()
) {
    suspend fun registerSubscriber(firstName: String, lastName: String, phone: String, email: String?) =
        call { api.registerSubscriber(SubscriberRegisterRequest(firstName, lastName, phone, email)) }

    suspend fun verifyOtp(phone: String, otpCode: String): AuthResult<AuthData> =
        call { api.verifyOtp(OtpVerifyRequest(phone, otpCode)) }.saveTokensOnSuccess()

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
                    userRole = value.user.role
                )
            )
        }
        return this
    }

    suspend fun clearLocalSession() {
        tokenStorage.clear()
    }

    suspend fun restoreLocalSession(): AuthUser? {
        val tokens = tokenStorage.read() ?: return null
        if (tokens.isAccessTokenExpired(Instant.now().epochSecond)) {
            tokenStorage.clear()
            return null
        }
        return AuthUser(
            id = tokens.userId,
            role = tokens.userRole
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

    private fun <T> parseError(response: Response<ApiResponse<T>>): ApiError? {
        val body = response.errorBody()?.string() ?: return null
        val type = object : TypeToken<ApiResponse<T>>() {}.type
        return runCatching { gson.fromJson<ApiResponse<T>>(body, type).error }.getOrNull()
    }
}
