package com.example.offerhub.data.model.admin

data class AdminStaff(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val specialties: List<String>,
    val regions: List<String>,
    val tempPassword: String? = null
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

/** UUID -> "Ad Soyad" cozumlemesi. Eslesme yoksa ham id geri doner, ekranda cig UUID kalmaz. */
fun List<AdminStaff>.displayNameFor(id: String?): String? {
    if (id.isNullOrBlank()) return null
    val staff = firstOrNull { it.id == id } ?: return id
    return "${staff.firstName} ${staff.lastName}"
}
