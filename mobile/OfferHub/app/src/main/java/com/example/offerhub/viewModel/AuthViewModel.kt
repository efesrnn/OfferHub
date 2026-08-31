package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.data.model.auth.AuthMode
import com.example.offerhub.data.model.auth.AuthUser
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.repository.AuthRepository
import com.example.offerhub.repository.AuthResult
import com.example.offerhub.R
import com.example.offerhub.BuildConfig
import com.example.offerhub.ui.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class AuthUiState(
    val isSessionChecking: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val currentUser: AuthUser? = null,
    val pendingNavigationRole: String? = null,
    val pendingPhone: String? = null,
    val otpReady: Boolean = false,
    val lockRemainingSeconds: Long = 0
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var lockJob: Job? = null

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val user = repository.restoreLocalSession()
            _uiState.update {
                it.copy(
                    isSessionChecking = false,
                    currentUser = user,
                    pendingNavigationRole = user?.role
                )
            }
        }
    }

    fun setPendingPhone(phone: String) {
        _uiState.update {
            it.copy(
                pendingPhone = phone,
                errorMessage = null
            )
        }
    }
    fun registerSubscriber(firstName: String, lastName: String, phone: String, email: String) =
        execute(
            operation = { repository.registerSubscriber(firstName, lastName, phone, email.ifBlank { null }, AuthMode.MOCK) },
            onSuccess = { _uiState.update { it.copy(pendingPhone = phone, otpReady = true) } }
        )

    fun verifyOtp(phone: String, otp: String, useFirebase: Boolean) = execute(
        operation = {
            val mode = if (useFirebase) AuthMode.FIREBASE else AuthMode.MOCK
            repository.verifyOtp(mode, phone, otp)
        },
        onSuccess = { data ->
            _uiState.update {
                it.copy(
                    currentUser = data.user,
                    pendingNavigationRole = data.user.role,
                    pendingPhone = data.user.phone ?: phone
                )
            }
        }
    )

    fun staffLogin(email: String, password: String) = execute(
        operation = { repository.staffLogin(email, password) },
        onSuccess = { data ->
            _uiState.update {
                it.copy(
                    currentUser = data.user,
                    pendingNavigationRole = data.user.role
                )
            }
        }
    )

    fun debugLoginAsAdmin() {
        if (!BuildConfig.DEBUG) return

        val debugAdmin = AuthUser(
            id = "debug-admin",
            role = "ADMIN"
        )
        _uiState.update {
            it.copy(
                currentUser = debugAdmin,
                pendingNavigationRole = debugAdmin.role,
                errorMessage = null
            )
        }
    }

    fun debugLoginAsExpert() {
        if (!BuildConfig.DEBUG) return

        val debugExpert = AuthUser(
            id = "debug-expert",
            role = "EXPERT",
            specialties = listOf("CHURN_ONLEME"),
            regions = listOf("ISTANBUL")
        )
        _uiState.update {
            it.copy(
                currentUser = debugExpert,
                pendingNavigationRole = debugExpert.role,
                errorMessage = null
            )
        }
    }

    fun debugLoginAsSupervisor() {
        if (!BuildConfig.DEBUG) return

        val debugSupervisor = AuthUser(
            id = "debug-supervisor",
            role = "SUPERVISOR",
            regions = listOf("ISTANBUL", "ANKARA")
        )
        _uiState.update {
            it.copy(
                currentUser = debugSupervisor,
                pendingNavigationRole = debugSupervisor.role,
                errorMessage = null
            )
        }
    }

    fun consumeOtpReady() = _uiState.update { it.copy(otpReady = false) }
    fun consumeAuthenticationNavigation() =
        _uiState.update { it.copy(pendingNavigationRole = null) }

    fun handleUnsupportedRole() {
        _uiState.update {
            it.copy(
                pendingNavigationRole = null,
                errorMessage = UiText.Resource(R.string.error_unsupported_role)
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearLocalSession()
            lockJob?.cancel()
            _uiState.value = AuthUiState(isSessionChecking = false)
        }
    }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun <T> execute(operation: suspend () -> AuthResult<T>, onSuccess: (T) -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = operation()) {
                is AuthResult.Success -> onSuccess(result.value)
                is AuthResult.Failure -> handleError(result.error)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleError(error: ApiError) {
        _uiState.update { it.copy(errorMessage = errorCodeMessage(error.code)) }
        if (error.code == "ACCOUNT_LOCKED") {
            val seconds = error.lockedUntil?.let {
                runCatching { Instant.parse(it).epochSecond - Instant.now().epochSecond }.getOrNull()
            } ?: error.remainingSeconds ?: 0L
            startBackendLockCountdown(seconds.coerceAtLeast(0L))
        }
    }

    private fun startBackendLockCountdown(initialSeconds: Long) {
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            var remaining = initialSeconds
            while (remaining > 0) {
                _uiState.update { it.copy(lockRemainingSeconds = remaining) }
                delay(1_000)
                remaining--
            }
            _uiState.update { it.copy(lockRemainingSeconds = 0) }
        }
    }

    private fun errorCodeMessage(code: String): UiText = UiText.Resource(when (code) {
        "INVALID_CREDENTIALS" -> R.string.error_invalid_credentials
        "ACCOUNT_LOCKED" -> R.string.error_account_locked
        "INVALID_OTP" -> R.string.error_invalid_otp_backend
        "DUPLICATE_RESOURCE", "PHONE_ALREADY_EXISTS", "SUBSCRIBER_ALREADY_EXISTS" ->
            R.string.error_phone_exists
        "EMAIL_ALREADY_EXISTS" -> R.string.error_email_exists
        "USER_NOT_FOUND", "SUBSCRIBER_NOT_FOUND" -> R.string.error_user_not_found
        "RATE_LIMITED", "TOO_MANY_REQUESTS" -> R.string.error_too_many_requests
        "NETWORK_ERROR" -> R.string.error_network
        else -> R.string.error_unknown
    })

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
    }
}
