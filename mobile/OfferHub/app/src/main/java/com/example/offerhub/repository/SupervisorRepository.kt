package com.example.offerhub.repository

import com.example.offerhub.data.model.supervisor.SupervisorDashboard
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment

sealed interface SupervisorResult<out T> {
    data class Success<T>(val value: T) : SupervisorResult<T>
    data class Failure(val error: ApiError) : SupervisorResult<Nothing>
}

interface SupervisorRepository {
    suspend fun getDashboard(): SupervisorResult<SupervisorDashboard>
    suspend fun assignCase(caseId: String, expertId: String): SupervisorResult<SupervisorDashboard>
    suspend fun publishCase(caseId: String): SupervisorResult<SupervisorDashboard>
    suspend fun updateCaseClassification(
        campaignNo: String,
        segment: Segment,
        priority: Priority,
        reason: String
    ): SupervisorResult<SupervisorDashboard>
}
