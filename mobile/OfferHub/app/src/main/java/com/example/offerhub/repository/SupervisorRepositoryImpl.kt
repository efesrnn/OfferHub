package com.example.offerhub.repository

import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.supervisor.SegmentDistribution
import com.example.offerhub.data.model.supervisor.SupervisorCaseSummary
import com.example.offerhub.data.model.supervisor.SupervisorDashboard
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.data.network.ApiResponse
import com.example.offerhub.data.remote.SupervisorApi
import com.example.offerhub.data.remote.dto.AssignCaseRequest
import com.example.offerhub.data.remote.dto.CaseDto
import com.example.offerhub.data.remote.dto.StatusChangeRequest
import com.example.offerhub.data.remote.dto.toSupervisorSummary
import com.example.offerhub.data.remote.dto.toSegmentDistribution
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

class SupervisorRepositoryImpl(
    private val api: SupervisorApi
) : SupervisorRepository {

    override suspend fun getDashboard(): SupervisorResult<SupervisorDashboard> {
        return try {
            when (val cases = loadAllCases()) {
                is SupervisorResult.Failure -> cases
                is SupervisorResult.Success -> loadDashboard(cases.value)
            }
        } catch (_: IOException) {
            SupervisorResult.Failure(ApiError("NETWORK_ERROR"))
        } catch (_: Exception) {
            SupervisorResult.Failure(ApiError("UNKNOWN_ERROR"))
        }
    }

    override suspend fun assignCase(
        caseId: String,
        expertId: String
    ): SupervisorResult<SupervisorDashboard> = actionThenReload {
        api.assignCase(caseId, AssignCaseRequest(expertId))
    }

    override suspend fun publishCase(caseId: String): SupervisorResult<SupervisorDashboard> =
        actionThenReload {
            api.changeCaseStatus(
                caseId,
                StatusChangeRequest(targetStatus = CaseStatus.YAYINDA.name)
            )
        }

    override suspend fun updateCaseClassification(
        caseId: String,
        segment: Segment,
        priority: Priority
    ): SupervisorResult<SupervisorDashboard> = SupervisorResult.Failure(
        ApiError(
            code = "ENDPOINT_NOT_AVAILABLE",
            message = "The backend does not expose a case classification endpoint."
        )
    )

    private suspend fun actionThenReload(
        action: suspend () -> Response<ApiResponse<CaseDto>>
    ): SupervisorResult<SupervisorDashboard> = try {
        val response = action()
        val envelope = response.body()
        if (response.isSuccessful && envelope?.success == true && envelope.data != null) {
            getDashboard()
        } else {
            SupervisorResult.Failure(errorFrom(response, envelope?.error))
        }
    } catch (_: IOException) {
        SupervisorResult.Failure(ApiError("NETWORK_ERROR"))
    } catch (_: Exception) {
        SupervisorResult.Failure(ApiError("UNKNOWN_ERROR"))
    }

    private suspend fun loadAllCases(): SupervisorResult<List<SupervisorCaseSummary>> {
        val result = mutableListOf<SupervisorCaseSummary>()
        var page = 0
        val size = 100
        var total = Long.MAX_VALUE
        var fetchedCount = 0L

        while (fetchedCount < total) {
            val response = api.getCases(page = page, size = size)
            val envelope = response.body()
            val data = envelope?.data
            if (!response.isSuccessful || envelope?.success != true || data == null) {
                return SupervisorResult.Failure(errorFrom(response, envelope?.error))
            }
            result += data.items.mapNotNull { it.toSupervisorSummary() }
            fetchedCount += data.items.size
            total = data.total
            if (data.items.isEmpty()) break
            page++
        }

        return SupervisorResult.Success(result)
    }

    private suspend fun loadDashboard(
        cases: List<SupervisorCaseSummary>
    ): SupervisorResult<SupervisorDashboard> {
        val dashboardResponse = api.getDashboard()
        val dashboardEnvelope = dashboardResponse.body()
        val dashboard = dashboardEnvelope?.data
        if (!dashboardResponse.isSuccessful || dashboardEnvelope?.success != true || dashboard == null) {
            return SupervisorResult.Failure(errorFrom(dashboardResponse, dashboardEnvelope?.error))
        }

        val accuracyResponse = api.getAiAccuracy()
        val accuracyEnvelope = accuracyResponse.body()
        val accuracy = accuracyEnvelope?.data?.overallAccuracy
        if (!accuracyResponse.isSuccessful || accuracyEnvelope?.success != true || accuracy == null) {
            return SupervisorResult.Failure(errorFrom(accuracyResponse, accuracyEnvelope?.error))
        }

        val conversionRate = dashboard.conversionRate
            ?: return SupervisorResult.Failure(ApiError("INVALID_RESPONSE"))
        val slaComplianceRate = dashboard.slaComplianceRate
            ?: return SupervisorResult.Failure(ApiError("INVALID_RESPONSE"))
        val breachedCases = dashboard.slaBreachedActiveCases
            ?: return SupervisorResult.Failure(ApiError("INVALID_RESPONSE"))
        val pendingQueueCount = dashboard.pendingQueueCount
            ?: return SupervisorResult.Failure(ApiError("INVALID_RESPONSE"))
        if (dashboard.segmentDistribution == null) {
            return SupervisorResult.Failure(ApiError("INVALID_RESPONSE"))
        }

        return SupervisorResult.Success(
            cases.toDashboard(
                accuracy = accuracy,
                conversionRate = conversionRate,
                slaComplianceRate = slaComplianceRate,
                breachedCases = breachedCases,
                pendingQueueCount = pendingQueueCount,
                segmentDistribution = dashboard.toSegmentDistribution()
            )
        )
    }

    private fun List<SupervisorCaseSummary>.toDashboard(
        accuracy: Double,
        conversionRate: Double,
        slaComplianceRate: Double,
        breachedCases: Long,
        pendingQueueCount: Long,
        segmentDistribution: List<SegmentDistribution>
    ): SupervisorDashboard {
        val activeStatuses = setOf(
            CaseStatus.ATANDI,
            CaseStatus.OPTIMIZE_EDILIYOR,
            CaseStatus.TEST_EDILIYOR
        )
        val activeCases = filter { it.status in activeStatuses }
        return SupervisorDashboard(
            aiAccuracyPercent = accuracy.toPercent(),
            conversionRatePercent = conversionRate.toPercent(),
            slaCompliancePercent = slaComplianceRate.toPercent(),
            slaBreachedActiveCaseCount = breachedCases,
            activeCaseCount = activeCases.size,
            pendingAssignmentCount = pendingQueueCount.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
            segmentDistribution = segmentDistribution,
            conversionTrend = emptyList(),
            attentionCases = this,
            expertPerformance = emptyList()
        )
    }

    private fun Double.toPercent(): Double = if (this in 0.0..1.0) this * 100.0 else this

    private fun errorFrom(response: Response<*>, bodyError: ApiError?): ApiError {
        if (bodyError != null) return bodyError
        return runCatching {
            Gson().fromJson(response.errorBody()?.string(), ErrorEnvelope::class.java).error
        }.getOrNull() ?: ApiError("UNKNOWN_ERROR")
    }

    private data class ErrorEnvelope(val error: ApiError?)
}
