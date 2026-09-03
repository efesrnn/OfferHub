package com.example.offerhub.data.remote

import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.network.PagedResult
import com.example.offerhub.data.remote.dto.AdminCreateStaffRequest
import com.example.offerhub.data.remote.dto.AdminCreateStaffResponse
import com.example.offerhub.data.remote.dto.AdminRoleUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @POST("api/v1/admin/staff")
    suspend fun createStaff(@Body request: AdminCreateStaffRequest): Response<ApiResponse<AdminCreateStaffResponse>>

    @GET("api/v1/admin/staff")
    suspend fun searchStaff(@Query("query") query: String?): Response<ApiResponse<List<AdminStaff>>>

    @GET("api/v1/admin/staff/{staffId}")
    suspend fun getStaff(@Path("staffId") staffId: String): Response<ApiResponse<AdminStaff>>

    @PATCH("api/v1/admin/staff/{staffId}/role")
    suspend fun updateRole(
        @Path("staffId") staffId: String,
        @Body request: AdminRoleUpdateRequest
    ): Response<ApiResponse<AdminStaff>>

    @GET("api/v1/admin/audit-logs")
    suspend fun getAuditLogs(
        @Query("actionQuery") actionQuery: String?,
        @Query("action") action: String?,
        @Query("result") result: String?,
        @Query("fromDate") fromDate: String?,
        @Query("toDate") toDate: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiResponse<PagedResult<AuditLog>>>
}
