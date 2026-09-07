package com.example.offerhub.data.remote.dto

import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.network.PagedResult

data class StaffCreateRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val specialties: List<String>,
    val regions: List<String>
)

data class StaffCreateResponseDto(
    val staffId: String?,
    val tempPasswordSent: Boolean?,
    val tempPassword: String?
)

data class RoleUpdateRequest(
    val role: String
)

data class StaffDto(
    val id: String?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val role: String?,
    val specialties: List<String>?,
    val regions: List<String>?
)

data class AuditLogDto(
    val id: String?,
    val userId: String?,
    val action: String?,
    val timestamp: String?,
    val ip: String?,
    val result: String?,
    val detail: String?
)

data class AdminPagedResponseDto<T>(
    val items: List<T>?,
    val total: Long?,
    val page: Int?,
    val size: Int?
)

fun StaffDto.toDomain(): AdminStaff? {
    val safeId = id?.takeIf(String::isNotBlank) ?: return null
    val safeFirstName = firstName?.takeIf(String::isNotBlank) ?: return null
    val safeLastName = lastName?.takeIf(String::isNotBlank) ?: return null
    val safeEmail = email?.takeIf(String::isNotBlank) ?: return null
    val safeRole = role?.takeIf(String::isNotBlank) ?: return null

    return AdminStaff(
        id = safeId,
        firstName = safeFirstName,
        lastName = safeLastName,
        email = safeEmail,
        role = safeRole,
        specialties = specialties.orEmpty(),
        regions = regions.orEmpty()
    )
}

fun AuditLogDto.toDomain(): AuditLog? {
    val safeId = id?.takeIf(String::isNotBlank) ?: return null
    val safeUserId = userId?.takeIf(String::isNotBlank) ?: return null
    val safeAction = action?.takeIf(String::isNotBlank) ?: return null
    val safeTimestamp = timestamp?.takeIf(String::isNotBlank) ?: return null
    val safeResult = result?.takeIf(String::isNotBlank) ?: return null

    return AuditLog(
        id = safeId,
        userId = safeUserId,
        action = safeAction,
        timestamp = safeTimestamp,
        ip = ip.orEmpty(),
        result = safeResult,
        detail = detail
    )
}

fun AdminPagedResponseDto<AuditLogDto>.toDomain(): PagedResult<AuditLog>? {
    val safeItems = items ?: return null
    val safeTotal = total?.takeIf { it >= 0 } ?: return null
    val safePage = page?.takeIf { it >= 0 } ?: return null
    val safeSize = size?.takeIf { it > 0 } ?: return null

    return PagedResult(
        items = safeItems.mapNotNull(AuditLogDto::toDomain),
        total = safeTotal,
        page = safePage,
        size = safeSize
    )
}
