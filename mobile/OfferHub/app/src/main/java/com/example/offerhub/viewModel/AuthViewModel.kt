package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.data.local.SessionEvents
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
    val pendingPasswordChangeNavigation: Boolean = false,
    val passwordChangeCompleted: Boolean = false,
    val pendingPhone: String? = null,
    val otpReady: Boolean = false,
    val isOtpRequestLoading: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val lockRemainingSeconds: Long = 0,
    val forgotPasswordEmail: String? = null,
    val forgotPasswordCodeSent: Boolean = false,
    val isForgotPasswordLoading: Boolean = false,
    val isResettingPassword: Boolean = false,
    val resetPasswordCompleted: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val sessionEvents: SessionEvents = SessionEvents()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var lockJob: Job? = null
    private var resendCooldownJob: Job? = null
    private var pendingCurrentPassword: String? = null

    init {
        restoreSession()
        viewModelScope.launch {
            sessionEvents.expired.collect {
                // A silent refresh failed somewhere in the app: the server no longer
                // recognizes this session, so drop back to the login screen the same way a
                // manual logout would rather than leaving stale screens up that just fail
                // every request from here on.
                logout()
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val session = repository.restoreLocalSession()
            _uiState.update {
                it.copy(
                    isSessionChecking = false,
                    currentUser = session?.user,
                    pendingNavigationRole = session?.user?.role
                        ?.takeUnless { session.passwordChangeRequired },
                    pendingPasswordChangeNavigation =
                        session?.passwordChangeRequired == true
                )
            }
        }
    }

    fun registerSubscriber(firstName: String, lastName: String, phone: String, email: String) =
        execute(
            operation = { repository.registerSubscriber(firstName, lastName, phone, email.ifBlank { null }, AuthMode.MOCK) },
            onSuccess = { data ->
                if (data.otpSent) {
                    _uiState.update {
                        it.copy(
                            pendingPhone = phone,
                            otpReady = true,
                            resendCooldownSeconds = RESEND_COOLDOWN_SECONDS
                        )
                    }
                    startResendCooldown()
                } else {
                    handleError(ApiError("OTP_SEND_FAILED"))
                }
            }
        )

    fun requestOtpForLogin(phone: String) = requestOtp(
        phone = phone,
        authMode = AuthMode.MOCK,
        navigateToVerification = true
    )

    fun resendOtp(phone: String) = requestOtp(
        phone = phone,
        authMode = AuthMode.MOCK,
        navigateToVerification = false
    )

    fun verifyOtp(phone: String, otp: String) = execute(
        operation = { repository.verifyOtp(AuthMode.MOCK, phone, otp) },
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
            pendingCurrentPassword = password.takeIf { data.user.mustChangePassword }
            _uiState.update {
                it.copy(
                    currentUser = data.user,
                    pendingNavigationRole = data.user.role
                        .takeUnless { data.user.mustChangePassword },
                    pendingPasswordChangeNavigation = data.user.mustChangePassword,
                    passwordChangeCompleted = false
                )
            }
        }
    )

    fun changePassword(newPassword: String, confirmPassword: String) {
        if (newPassword != confirmPassword) {
            handleError(ApiError("PASSWORD_MISMATCH"))
            return
        }
        val currentPassword = pendingCurrentPassword ?: run {
            handleError(ApiError("INVALID_CREDENTIALS"))
            return
        }
        execute(
            operation = { repository.changePassword(currentPassword, newPassword) },
            onSuccess = {
                pendingCurrentPassword = null
                _uiState.update {
                    it.copy(
                        currentUser = null,
                        pendingNavigationRole = null,
                        passwordChangeCompleted = true
                    )
                }
            }
        )
    }

    fun requestPasswordReset(email: String) {
        if (_uiState.value.isForgotPasswordLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isForgotPasswordLoading = true, errorMessage = null) }
            when (val result = repository.forgotPassword(email)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(
                        isForgotPasswordLoading = false,
                        forgotPasswordEmail = email,
                        forgotPasswordCodeSent = true
                    )
                }
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isForgotPasswordLoading = false) }
                    handleError(result.error)
                }
            }
        }
    }

    fun consumeForgotPasswordNavigation() =
        _uiState.update { it.copy(forgotPasswordCodeSent = false) }

    fun resetPassword(code: String, newPassword: String) {
        val email = _uiState.value.forgotPasswordEmail ?: return
        if (_uiState.value.isResettingPassword) return
        viewModelScope.launch {
            _uiState.update { it.copy(isResettingPassword = true, errorMessage = null) }
            when (val result = repository.resetPassword(email, code, newPassword)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isResettingPassword = false, resetPasswordCompleted = true)
                }
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isResettingPassword = false) }
                    handleError(result.error)
                }
            }
        }
    }

    fun finishResetPasswordFlow() = _uiState.update {
        it.copy(
            resetPasswordCompleted = false,
            forgotPasswordEmail = null,
            forgotPasswordCodeSent = false,
            errorMessage = null
        )
    }

    fun debugLoginAsAdmin() {
        if (!BuildConfig.USE_MOCK_ADMIN) return

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

    fun debugLoginAsSubscriber() {
        if (!BuildConfig.USE_MOCK_SUBSCRIBER) return

        val debugSubscriber = AuthUser(
            id = "debug-subscriber",
            role = "SUBSCRIBER",
            phone = "5551234567"
        )
        _uiState.update {
            it.copy(
                currentUser = debugSubscriber,
                pendingNavigationRole = debugSubscriber.role,
                pendingPhone = debugSubscriber.phone,
                errorMessage = null
            )
        }
    }

    fun debugLoginAsExpert() {
        if (!BuildConfig.USE_MOCK_EXPERT) return

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
        if (!BuildConfig.USE_MOCK_SUPERVISOR) return

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

    fun consumePasswordChangeNavigation() =
        _uiState.update { it.copy(pendingPasswordChangeNavigation = false) }

    fun finishPasswordChangeFlow() =
        _uiState.update {
            it.copy(
                passwordChangeCompleted = false,
                errorMessage = null
            )
        }

    fun cancelPasswordChange() {
        viewModelScope.launch {
            pendingCurrentPassword = null
            repository.clearLocalSession()
            _uiState.value = AuthUiState(isSessionChecking = false)
        }
    }

    fun handleUnsupportedRole() {
        _uiState.update {
            it.copy(
                pendingNavigationRole = null,
                errorMessage = UiText.Resource(R.string.error_unsupported_role)
            )
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            pendingCurrentPassword = null
            repository.logout()
            lockJob?.cancel()
            resendCooldownJob?.cancel()
            _uiState.value = AuthUiState(isSessionChecking = false)
            onComplete()
        }
    }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun requestOtp(
        phone: String,
        authMode: AuthMode,
        navigateToVerification: Boolean
    ) {
        if (_uiState.value.isOtpRequestLoading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isOtpRequestLoading = true, errorMessage = null)
            }
            when (val result = repository.requestOtp(phone, authMode)) {
                is AuthResult.Success -> {
                    if (result.value.otpSent) {
                        _uiState.update {
                            it.copy(
                                pendingPhone = phone,
                                otpReady = navigateToVerification,
                                isOtpRequestLoading = false,
                                resendCooldownSeconds = RESEND_COOLDOWN_SECONDS
                            )
                        }
                        startResendCooldown()
                    } else {
                        _uiState.update { it.copy(isOtpRequestLoading = false) }
                        handleError(ApiError("OTP_SEND_FAILED"))
                    }
                }
                is AuthResult.Failure -> {
                    val error = if (result.error.code == "INVALID_OTP") {
                        result.error.copy(code = "SUBSCRIBER_NOT_FOUND")
                    } else {
                        result.error
                    }
                    _uiState.update { it.copy(isOtpRequestLoading = false) }
                    handleError(error)
                }
            }
        }
    }

    private fun startResendCooldown() {
        resendCooldownJob?.cancel()
        resendCooldownJob = viewModelScope.launch {
            while (_uiState.value.resendCooldownSeconds > 0) {
                delay(1_000)
                _uiState.update {
                    it.copy(
                        resendCooldownSeconds =
                            (it.resendCooldownSeconds - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

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
        "OTP_SEND_FAILED" -> R.string.error_otp_send_failed
        "WEAK_PASSWORD" -> R.string.error_weak_password
        "PASSWORD_MISMATCH" -> R.string.error_password_mismatch
        "DUPLICATE_RESOURCE", "PHONE_ALREADY_EXISTS", "SUBSCRIBER_ALREADY_EXISTS" ->
            R.string.error_phone_exists
        "EMAIL_ALREADY_EXISTS" -> R.string.error_email_exists
        "SUBSCRIBER_NOT_FOUND" -> R.string.error_subscriber_phone_not_found
        "USER_NOT_FOUND", "NOT_FOUND" -> R.string.error_user_not_found
        "RATE_LIMITED", "TOO_MANY_REQUESTS" -> R.string.error_too_many_requests
        "NETWORK_ERROR" -> R.string.error_network
        else -> R.string.error_unknown
    })

    class Factory(
        private val repository: AuthRepository,
        private val sessionEvents: SessionEvents = SessionEvents()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(repository, sessionEvents) as T
    }

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 30
    }
}
