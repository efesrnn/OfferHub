package com.example.offerhub.data.remote

import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.remote.dto.AdminPagedResponseDto
import com.example.offerhub.data.remote.dto.AuditLogDto
import com.example.offerhub.data.remote.dto.RoleUpdateRequest
import com.example.offerhub.data.remote.dto.StaffCreateRequest
import com.example.offerhub.data.remote.dto.StaffCreateResponseDto
import com.example.offerhub.data.remote.dto.StaffDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @POST("api/v1/admin/staff")
    suspend fun createStaff(
        @Body request: StaffCreateRequest
    ): Response<ApiResponse<StaffCreateResponseDto>>

    @GET("api/v1/admin/staff")
    suspend fun searchStaff(
        @Query("query") query: String?
    ): Response<ApiResponse<List<StaffDto>>>

    @GET("api/v1/admin/staff/{staffId}")
    suspend fun getStaff(
        @Path("staffId") staffId: String
    ): Response<ApiResponse<StaffDto>>

    @PATCH("api/v1/admin/staff/{staffId}/role")
    suspend fun updateRole(
        @Path("staffId") staffId: String,
        @Body request: RoleUpdateRequest
    ): Response<ApiResponse<StaffDto>>

    @GET("api/v1/admin/audit-logs")
    suspend fun getAuditLogs(
        @Query("actionQuery") actionQuery: String?,
        @Query("action") action: String?,
        @Query("result") result: String?,
        @Query("fromDate") fromDate: String?,
        @Query("toDate") toDate: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiResponse<AdminPagedResponseDto<AuditLogDto>>>
}
