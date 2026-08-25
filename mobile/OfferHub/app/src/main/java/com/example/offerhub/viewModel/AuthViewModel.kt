package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.data.model.auth.AuthMode
import com.example.offerhub.data.model.auth.AuthUser
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.repository.AuthRepository
import com.example.offerhub.repository.AuthResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedUser: AuthUser? = null,
    val pendingPhone: String? = null,
    val otpReady: Boolean = false,
    val lockRemainingSeconds: Long = 0
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var lockJob: Job? = null

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
        onSuccess = { data -> _uiState.update { it.copy(authenticatedUser = data.user) } }
    )

    fun staffLogin(email: String, password: String) = execute(
        operation = { repository.staffLogin(email, password) },
        onSuccess = { data -> _uiState.update { it.copy(authenticatedUser = data.user) } }
    )

    fun consumeOtpReady() = _uiState.update { it.copy(otpReady = false) }
    fun consumeAuthentication() = _uiState.update { it.copy(authenticatedUser = null) }
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

    private fun errorCodeMessage(code: String): String = when (code) {
        "INVALID_CREDENTIALS" -> "E-posta veya şifre hatalı."
        "ACCOUNT_LOCKED" -> "Çok fazla başarısız deneme nedeniyle hesap geçici olarak kilitlendi."
        "INVALID_OTP" -> "Doğrulama kodu geçersiz veya süresi dolmuş."
        "PHONE_ALREADY_EXISTS", "SUBSCRIBER_ALREADY_EXISTS" -> "Bu telefon numarası zaten kayıtlı."
        "EMAIL_ALREADY_EXISTS" -> "Bu e-posta adresi zaten kullanılıyor."
        "USER_NOT_FOUND", "SUBSCRIBER_NOT_FOUND" -> "Bu bilgilerle eşleşen bir hesap bulunamadı."
        "RATE_LIMITED", "TOO_MANY_REQUESTS" -> "Çok fazla istek gönderildi. Lütfen biraz bekleyin."
        "NETWORK_ERROR" -> "Ağ bağlantısı kurulamadı. Lütfen bağlantınızı kontrol edin."
        else -> "İşlem tamamlanamadı. Lütfen tekrar deneyin."
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
    }
}

