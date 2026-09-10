package com.example.offerhub.data.remote

import com.example.offerhub.data.local.AccessTokenProvider
import com.example.offerhub.data.local.SessionEvents
import com.example.offerhub.repository.AuthRepository
import com.example.offerhub.repository.AuthResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Makes the access token's 15-minute expiry invisible to the rest of the app: when any API
 * call comes back 401, this refreshes it once (via the stored refresh token) and retries the
 * original request, instead of the screen that made the call ever seeing the 401.
 *
 * One instance is shared by every OkHttpClient the app builds except the auth one (see
 * ApiClient - the auth client must not authenticate itself, or a failing /refresh call would
 * try to refresh its way out of failing). Its mutex serializes refresh attempts across all of
 * them: the backend's refresh tokens are one-time-use, so two screens hitting 401 at the same
 * moment must not each call /refresh independently - the second call would find the first
 * call's token already spent and treat it as a replayed/stolen token, which revokes every
 * session for the user, not just this request's.
 */
class TokenAuthenticator(
    private val authRepository: AuthRepository,
    private val tokenProvider: AccessTokenProvider,
    private val sessionEvents: SessionEvents
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Nothing we attached a bearer token to - a refresh cannot be the fix for this 401.
        if (response.request.header(AUTHORIZATION) == null) return null
        // Already retried once for this request chain: refreshing again would not help,
        // and without this OkHttp would keep looping if the server always answers 401.
        if (responseCount(response) >= MAX_ATTEMPTS) return null

        val failedAccessToken = response.request.header(AUTHORIZATION)?.removePrefix("Bearer ")

        return runBlocking {
            mutex.withLock {
                val currentAccessToken = tokenProvider.accessToken()
                val accessToken = if (currentAccessToken != null && currentAccessToken != failedAccessToken) {
                    // Another request already refreshed while this one waited for the lock -
                    // reuse that instead of spending a second refresh token.
                    currentAccessToken
                } else {
                    when (val result = authRepository.refresh()) {
                        is AuthResult.Success -> result.value.accessToken
                        is AuthResult.Failure -> {
                            sessionEvents.notifyExpired()
                            null
                        }
                    }
                }

                accessToken?.let {
                    response.request.newBuilder()
                        .header(AUTHORIZATION, "Bearer $it")
                        .build()
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val MAX_ATTEMPTS = 2
    }
}
