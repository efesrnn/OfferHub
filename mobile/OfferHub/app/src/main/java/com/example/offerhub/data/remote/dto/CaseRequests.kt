package com.example.offerhub.data.remote.dto

data class StatusChangeRequest(
    val targetStatus: String,
    val optimizationNote: String? = null
)

data class AssignCaseRequest(
    val expertId: String
)
