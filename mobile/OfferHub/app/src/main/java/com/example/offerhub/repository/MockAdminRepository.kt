package com.example.offerhub.repository

import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.PagedResult
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MockAdminRepository : AdminRepository {
    private val staff = mutableListOf<AdminStaff>()
    private val auditLogs = List(53) { index ->
        val failed = index % 4 == 0
        AuditLog(
            id = "audit-${index + 1}",
            userId = "staff-${(index % 8) + 1}",
            action = if (failed) "LOGIN_FAILED" else listOf("LOGIN_SUCCESS", "STAFF_CREATED", "ROLE_UPDATED")[index % 3],
            timestamp = Instant.now().minus(index.toLong(), ChronoUnit.HOURS).toString(),
            ip = "192.168.1.${(index % 20) + 10}",
            result = if (failed) "FAILED" else "SUCCESS",
            detail = if (failed) "Invalid staff credentials" else null
        )
    }.toMutableList()

    override suspend fun createStaff(
        firstName: String,
        lastName: String,
        email: String,
        role: String,
        specialties: List<String>,
        regions: List<String>
    ): AdminResult<AdminStaff> {
        delay(500)
        if (staff.any { it.email.equals(email, ignoreCase = true) }) {
            return AdminResult.Failure(ApiError("DUPLICATE_RESOURCE", "Email already exists"))
        }
        val created = AdminStaff(UUID.randomUUID().toString(), firstName, lastName, email, role, specialties, regions)
        staff += created
        addAuditLog(
            userId = created.id,
            action = "STAFF_CREATED",
            detail = "$firstName $lastName created with role $role"
        )
        return AdminResult.Success(created)
    }

    override suspend fun updateRole(staffId: String, role: String): AdminResult<AdminStaff> {
        delay(400)
        val index = staff.indexOfFirst { it.id == staffId }
        if (index == -1) return AdminResult.Failure(ApiError("NOT_FOUND", "Staff member not found"))
        if (staff[index].role == role) {
            return AdminResult.Failure(ApiError("VALIDATION_ERROR", "Staff member already has this role"))
        }
        val updated = staff[index].copy(role = role)
        staff[index] = updated
        addAuditLog(
            userId = updated.id,
            action = "ROLE_UPDATED",
            detail = "Role changed to $role"
        )
        return AdminResult.Success(updated)
    }

    override suspend fun findStaff(staffId: String): AdminResult<AdminStaff> {
        delay(250)
        val found = staff.firstOrNull { it.id == staffId }
            ?: return AdminResult.Failure(ApiError("NOT_FOUND", "Staff member not found"))
        return AdminResult.Success(found)
    }

    override suspend fun searchStaff(query: String): AdminResult<List<AdminStaff>> {
        delay(300)
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return AdminResult.Success(emptyList())

        return AdminResult.Success(
            staff.filter { member ->
                member.firstName.contains(normalizedQuery, ignoreCase = true) ||
                    member.lastName.contains(normalizedQuery, ignoreCase = true) ||
                    "${member.firstName} ${member.lastName}".contains(normalizedQuery, ignoreCase = true)
            }
        )
    }

    override suspend fun getAuditLogs(
        actionQuery: String?,
        action: String?,
        result: String?,
        fromDate: String?,
        toDate: String?,
        page: Int,
        size: Int
    ): AdminResult<PagedResult<AuditLog>> {
        delay(350)
        val filtered = auditLogs.filter { log ->
            val matchesQuery = actionQuery.isNullOrBlank() ||
                log.action.contains(actionQuery, ignoreCase = true) ||
                log.userId.contains(actionQuery, ignoreCase = true) ||
                log.ip.contains(actionQuery, ignoreCase = true)
            val matchesAction = action.isNullOrBlank() || log.action == action
            val matchesResult = result.isNullOrBlank() || log.result == result
            val logDate = log.timestamp.substringBefore('T')
            val matchesFromDate = fromDate.isNullOrBlank() || logDate >= fromDate
            val matchesToDate = toDate.isNullOrBlank() || logDate <= toDate
            matchesQuery && matchesAction && matchesResult && matchesFromDate && matchesToDate
        }
        val fromIndex = page * size
        val items = if (fromIndex >= filtered.size) emptyList() else filtered.drop(fromIndex).take(size)
        return AdminResult.Success(PagedResult(items, filtered.size.toLong(), page, size))
    }

    private fun addAuditLog(
        userId: String,
        action: String,
        detail: String
    ) {
        auditLogs.add(
            index = 0,
            element = AuditLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                action = action,
                timestamp = Instant.now().toString(),
                ip = "127.0.0.1",
                result = "SUCCESS",
                detail = detail
            )
        )
    }
}
