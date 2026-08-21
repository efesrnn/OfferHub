package com.example.offerhub.data.network

data class ApiResponse<T>(val success: Boolean, val data: T?, val error: ApiError?)

data class ApiError(
    val code: String,
    val message: String? = null,
    val lockedUntil: String? = null,
    val remainingSeconds: Long? = null
)

data class PagedResult<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int
)
