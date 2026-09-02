package com.example.offerhub.repository

import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.network.PagedResult
import com.example.offerhub.data.remote.AdminApi
import com.example.offerhub.data.remote.dto.AdminCreateStaffRequest
import com.example.offerhub.data.remote.dto.AdminRoleUpdateRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import java.io.IOException

class AdminRepositoryImpl(
    private val api: AdminApi,
    private val gson: Gson = Gson()
) : AdminRepository {

    override suspend fun createStaff(
        firstName: String,
        lastName: String,
        email: String,
        role: String,
        specialties: List<String>,
        regions: List<String>
    ): AdminResult<AdminStaff> = call {
        api.createStaff(AdminCreateStaffRequest(firstName, lastName, email, role, specialties, regions))
    }.map { response ->
        AdminStaff(
            id = response.staffId,
            firstName = firstName,
            lastName = lastName,
            email = email,
            role = role,
            specialties = specialties,
            regions = regions,
            tempPassword = response.tempPassword
        )
    }

    override suspend fun updateRole(staffId: String, role: String): AdminResult<AdminStaff> =
        call { api.updateRole(staffId, AdminRoleUpdateRequest(role)) }

    override suspend fun findStaff(staffId: String): AdminResult<AdminStaff> =
        call { api.getStaff(staffId) }

    override suspend fun searchStaff(query: String): AdminResult<List<AdminStaff>> =
        call { api.searchStaff(query.ifBlank { null }) }

    override suspend fun getAuditLogs(
        actionQuery: String?,
        action: String?,
        result: String?,
        fromDate: String?,
        toDate: String?,
        page: Int,
        size: Int
    ): AdminResult<PagedResult<AuditLog>> = call {
        api.getAuditLogs(actionQuery, action, result, fromDate, toDate, page, size)
    }

    private suspend fun <T> call(block: suspend () -> Response<ApiResponse<T>>): AdminResult<T> = try {
        val response = block()
        val envelope = response.body()
        if (response.isSuccessful && envelope?.success == true && envelope.data != null) {
            AdminResult.Success(envelope.data)
        } else {
            AdminResult.Failure(envelope?.error ?: parseError(response) ?: ApiError("UNKNOWN_ERROR"))
        }
    } catch (_: IOException) {
        AdminResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        AdminResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    private fun <T> AdminResult<T>.map(transform: (T) -> AdminStaff): AdminResult<AdminStaff> =
        when (this) {
            is AdminResult.Success -> AdminResult.Success(transform(value))
            is AdminResult.Failure -> this
        }

    private fun <T> parseError(response: Response<ApiResponse<T>>): ApiError? {
        val body = response.errorBody()?.string() ?: return null
        val type = object : TypeToken<ApiResponse<T>>() {}.type
        return runCatching { gson.fromJson<ApiResponse<T>>(body, type).error }.getOrNull()
    }
}
