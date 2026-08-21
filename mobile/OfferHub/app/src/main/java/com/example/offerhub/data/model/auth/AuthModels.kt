package com.example.offerhub.data.model.auth

data class SubscriberRegisterRequest(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String?
)

data class SubscriberRegisterData(val subscriberId: String? = null, val otpSent: Boolean)
data class OtpVerifyRequest(val phone: String, val otpCode: String)
data class StaffLoginRequest(val email: String, val password: String)

data class AuthData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: AuthUser
)

data class AuthUser(
    val id: String,
    val role: String,
    val specialties: List<String> = emptyList(),
    val regions: List<String> = emptyList()
)
