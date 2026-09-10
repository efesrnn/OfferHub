package com.example.offerhub.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * App-wide signal for "the refresh token no longer works, so as far as the server is
 * concerned this session is over."
 *
 * TokenAuthenticator emits into this when a silent refresh fails; AuthViewModel listens so it
 * can drop the app back to the login screen the same way a manual logout does, instead of
 * every subsequent screen just quietly failing its requests one by one.
 */
class SessionEvents {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
