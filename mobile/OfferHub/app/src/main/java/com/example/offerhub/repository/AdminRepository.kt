package com.example.offerhub.repository

import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.PagedResult

sealed interface AdminResult<out T> {
    data class Success<T>(val value: T) : AdminResult<T>
    data class Failure(val error: ApiError) : AdminResult<Nothing>
}

interface AdminRepository {
    suspend fun createStaff(
        firstName: String,
        lastName: String,
        email: String,
        role: String,
        specialties: List<String>,
        regions: List<String>
    ): AdminResult<AdminStaff>

    suspend fun updateRole(staffId: String, role: String): AdminResult<AdminStaff>

    suspend fun findStaff(staffId: String): AdminResult<AdminStaff>

    suspend fun searchStaff(query: String): AdminResult<List<AdminStaff>>

    suspend fun getAuditLogs(
        actionQuery: String?,
        action: String?,
        result: String?,
        fromDate: String?,
        toDate: String?,
        page: Int,
        size: Int
    ): AdminResult<PagedResult<AuditLog>>
}
