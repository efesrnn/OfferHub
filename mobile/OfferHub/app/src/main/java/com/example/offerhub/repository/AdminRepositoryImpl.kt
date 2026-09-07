package com.example.offerhub.repository

import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.network.PagedResult
import com.example.offerhub.data.remote.AdminApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.offerhub.data.remote.dto.RoleUpdateRequest
import com.example.offerhub.data.remote.dto.StaffCreateRequest
import com.example.offerhub.data.remote.dto.toDomain
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
    ): AdminResult<AdminStaff> = try {
        val response = api.createStaff(
            StaffCreateRequest(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                email = email.trim(),
                role = role,
                specialties = specialties,
                regions = regions
            )
        )
        val envelope = response.body()
        val staffId = envelope?.data?.staffId?.takeIf(String::isNotBlank)

        if (response.isSuccessful && envelope?.success == true && staffId != null) {
            findStaff(staffId)
        } else {
            AdminResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        AdminResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        AdminResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    override suspend fun updateRole(
        staffId: String,
        role: String
    ): AdminResult<AdminStaff> = call(
        request = { api.updateRole(staffId.trim(), RoleUpdateRequest(role)) },
        transform = { it.toDomain() }
    )

    override suspend fun findStaff(staffId: String): AdminResult<AdminStaff> = call(
        request = { api.getStaff(staffId.trim()) },
        transform = { it.toDomain() }
    )

    override suspend fun searchStaff(query: String): AdminResult<List<AdminStaff>> {
        val normalizedQuery = query.trim()
        return call(
            request = { api.searchStaff(normalizedQuery.ifBlank { null }) },
            transform = { staff ->
                staff.mapNotNull { it.toDomain() }.filter { member ->
                    normalizedQuery.isBlank() ||
                        member.firstName.contains(normalizedQuery, ignoreCase = true) ||
                        member.lastName.contains(normalizedQuery, ignoreCase = true) ||
                        "${member.firstName} ${member.lastName}"
                            .contains(normalizedQuery, ignoreCase = true)
                }
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
    ): AdminResult<PagedResult<AuditLog>> = call(
        request = {
            api.getAuditLogs(
                actionQuery = actionQuery.nullIfBlank(),
                action = action.nullIfBlank(),
                result = result.nullIfBlank(),
                fromDate = fromDate.nullIfBlank(),
                toDate = toDate.nullIfBlank(),
                page = page,
                size = size
            )
        },
        transform = { it.toDomain() }
    )

    private suspend fun <Dto, Domain> call(
        request: suspend () -> Response<ApiResponse<Dto>>,
        transform: (Dto) -> Domain?
    ): AdminResult<Domain> = try {
        val response = request()
        val envelope = response.body()
        val value = envelope?.data?.let(transform)

        if (response.isSuccessful && envelope?.success == true && value != null) {
            AdminResult.Success(value)
        } else {
            AdminResult.Failure(errorFrom(response, envelope?.error))
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
    private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf(String::isNotBlank)

    private fun errorFrom(response: Response<*>, bodyError: ApiError?): ApiError {
        if (bodyError != null) return bodyError
        return runCatching {
            Gson().fromJson(response.errorBody()?.string(), ErrorEnvelope::class.java).error
        }.getOrNull() ?: ApiError("UNKNOWN_ERROR")
    }

    private data class ErrorEnvelope(val error: ApiError?)
}
