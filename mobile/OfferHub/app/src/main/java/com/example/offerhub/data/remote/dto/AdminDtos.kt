package com.example.offerhub.data.remote.dto

data class AdminCreateStaffRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val specialties: List<String>,
    val regions: List<String>
)

data class AdminCreateStaffResponse(
    val staffId: String,
    val tempPasswordSent: Boolean,
    val tempPassword: String?
)

data class AdminRoleUpdateRequest(
    val role: String
)
