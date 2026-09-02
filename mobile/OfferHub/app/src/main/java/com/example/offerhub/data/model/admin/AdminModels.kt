package com.example.offerhub.data.model.admin

data class AdminStaff(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val specialties: List<String>,
    val regions: List<String>
)

data class AuditLog(
    val id: String,
    val userId: String,
    val action: String,
    val timestamp: String,
    val ip: String,
    val result: String,
    val detail: String? = null
)
