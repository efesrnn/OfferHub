package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.R
import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.model.supervisor.SupervisorDashboard
import com.example.offerhub.repository.SupervisorRepository
import com.example.offerhub.repository.SupervisorResult
import com.example.offerhub.ui.text.UiText
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupervisorUiState(
    val dashboard: SupervisorDashboard? = null,
    val experts: List<AdminStaff> = emptyList(),
    val staffDirectory: List<AdminStaff> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isSubmittingAction: Boolean = false,
    val actionErrorMessage: UiText? = null,
    val actionSuccessVersion: Long = 0L,
    val selectedConversionTrendPeriod: String? = null
)

class SupervisorViewModel(
    private val repository: SupervisorRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupervisorUiState())
    val uiState: StateFlow<SupervisorUiState> = _uiState.asStateFlow()
    private val _snackbarEvents = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val snackbarEvents: SharedFlow<UiText> = _snackbarEvents

    fun loadDashboard() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getDashboard()) {
                is SupervisorResult.Success -> _uiState.update {
                    val selectedPeriod = it.selectedConversionTrendPeriod
                        ?.takeIf { period ->
                            result.value.conversionTrend.any { point -> point.period == period }
                        }
                        ?: result.value.conversionTrend.lastOrNull()?.period
                    it.copy(
                        dashboard = result.value,
                        isLoading = false,
                        selectedConversionTrendPeriod = selectedPeriod
                    )
                }
                is SupervisorResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = UiText.Resource(R.string.error_supervisor_dashboard))
                }
            }
        }
        loadExperts()
        loadStaffDirectory()
    }

    fun loadExperts() {
        viewModelScope.launch {
            when (val result = repository.getExperts()) {
                is SupervisorResult.Success -> _uiState.update { it.copy(experts = result.value) }
                is SupervisorResult.Failure -> Unit
            }
        }
    }

    fun loadStaffDirectory() {
        viewModelScope.launch {
            when (val result = repository.getStaffDirectory()) {
                is SupervisorResult.Success -> _uiState.update { it.copy(staffDirectory = result.value) }
                is SupervisorResult.Failure -> Unit
            }
        }
    }

    fun selectConversionTrendPeriod(period: String) {
        _uiState.update { it.copy(selectedConversionTrendPeriod = period) }
    }

    fun assignCase(caseId: String, expertId: String) = executeAction(
        successMessage = UiText.Resource(R.string.supervisor_case_assigned_success)
    ) {
        repository.assignCase(caseId, expertId)
    }

    fun publishCase(caseId: String) = executeAction(
        successMessage = UiText.Resource(R.string.supervisor_case_published_success)
    ) {
        repository.publishCase(caseId)
    }

    fun updateCaseClassification(
        campaignNo: String,
        segment: Segment,
        priority: Priority,
        reason: String
    ) = executeAction(
        successMessage = UiText.Resource(R.string.supervisor_classification_updated_success)
    ) {
        repository.updateCaseClassification(campaignNo, segment, priority, reason)
    }

    fun clearActionError() = _uiState.update { it.copy(actionErrorMessage = null) }

    private fun executeAction(
        successMessage: UiText,
        operation: suspend () -> SupervisorResult<SupervisorDashboard>
    ) {
        if (_uiState.value.isSubmittingAction) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, actionErrorMessage = null) }
            when (val result = operation()) {
                is SupervisorResult.Success -> {
                    _uiState.update {
                        it.copy(
                            dashboard = result.value,
                            isSubmittingAction = false,
                            actionSuccessVersion = it.actionSuccessVersion + 1
                        )
                    }
                    _snackbarEvents.tryEmit(successMessage)
                }
                is SupervisorResult.Failure -> _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        actionErrorMessage = UiText.Resource(
                            when (result.error.code) {
                                "EXPERT_CAPACITY_FULL" -> R.string.error_expert_capacity_full
                                "INVALID_STATUS_TRANSITION" -> R.string.error_invalid_case_transition
                                "RISK_SEGMENT_PRIORITY_TOO_LOW" -> R.string.error_risk_priority
                                "CASE_CLASSIFICATION_LOCKED" -> R.string.error_case_classification_locked
                                else -> R.string.error_supervisor_action
                            }
                        )
                    )
                }
            }
        }
    }

    class Factory(private val repository: SupervisorRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SupervisorViewModel(repository) as T
    }
}
