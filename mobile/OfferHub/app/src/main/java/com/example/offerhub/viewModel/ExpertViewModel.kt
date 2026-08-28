package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.data.model.campaign.OptimizationCase
import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.Campaign
import com.example.offerhub.data.model.campaign.CampaignType
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.CampaignStatus
import com.example.offerhub.repository.ExpertRepository
import com.example.offerhub.repository.ExpertResult
import com.example.offerhub.R
import com.example.offerhub.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpertUiState(
    val cases: List<OptimizationCase> = emptyList(),
    val page: Int = 0,
    val total: Long = 0,
    val caseStatusFilter: CaseStatus? = null,
    val isLoading: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val errorMessage: UiText? = null,
    val selectedCase: OptimizationCase? = null,
    val isLoadingDetail: Boolean = false,
    val detailErrorMessage: UiText? = null,
    val isDetailNotFound: Boolean = false,
    val isSubmittingAction: Boolean = false,
    val actionErrorMessage: UiText? = null,
    val campaigns: List<Campaign> = emptyList(),
    val campaignPage: Int = 0,
    val campaignTotal: Long = 0,
    val isLoadingCampaigns: Boolean = false,
    val isLoadingNextCampaignPage: Boolean = false,
    val campaignErrorMessage: UiText? = null,
    val selectedCampaign: Campaign? = null,
    val isLoadingCampaignDetail: Boolean = false,
    val isCreatingCampaign: Boolean = false,
    val campaignActionError: UiText? = null,
    val createdCampaignNo: String? = null,
    val campaignStatusFilter: CampaignStatus? = null,
    val campaignSegmentFilter: Segment? = null
) {
    val canLoadMore: Boolean get() = cases.size < total
    val canLoadMoreCampaigns: Boolean get() = campaigns.size < campaignTotal
}

class ExpertViewModel(private val repository: ExpertRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpertUiState())
    val uiState: StateFlow<ExpertUiState> = _uiState.asStateFlow()

    init { loadCases() }

    fun loadCases(
        reset: Boolean = true,
        status: CaseStatus? = _uiState.value.caseStatusFilter
    ) {
        val current = _uiState.value
        if (current.isLoading || current.isLoadingNextPage) return
        if (!reset && !current.canLoadMore) return
        val requestedPage = if (reset) 0 else current.page + 1

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = reset,
                    isLoadingNextPage = !reset,
                    errorMessage = null,
                    caseStatusFilter = status,
                    cases = if (reset) emptyList() else it.cases
                )
            }
            when (val result = repository.getAssignedCases(requestedPage, PAGE_SIZE, status)) {
                is ExpertResult.Success -> _uiState.update {
                    it.copy(
                        cases = if (reset) result.value.items else it.cases + result.value.items,
                        page = result.value.page,
                        total = result.value.total,
                        isLoading = false,
                        isLoadingNextPage = false
                    )
                }
                is ExpertResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingNextPage = false,
                        errorMessage = errorMessage(result.error.code)
                    )
                }
            }
        }
    }

    fun loadCaseDetail(caseId: String) {
        if (_uiState.value.isLoadingDetail) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedCase = null,
                    isLoadingDetail = true,
                    detailErrorMessage = null,
                    isDetailNotFound = false,
                    actionErrorMessage = null
                )
            }
            when (val result = repository.getCaseDetail(caseId)) {
                is ExpertResult.Success -> _uiState.update {
                    it.copy(selectedCase = result.value, isLoadingDetail = false)
                }
                is ExpertResult.Failure -> _uiState.update {
                    it.copy(
                        isLoadingDetail = false,
                        detailErrorMessage = errorMessage(result.error.code),
                        isDetailNotFound = result.error.code == "NOT_FOUND"
                    )
                }
            }
        }
    }

    fun changeCaseStatus(targetStatus: CaseStatus, optimizationNote: String? = null) {
        val selectedCase = _uiState.value.selectedCase ?: return
        if (_uiState.value.isSubmittingAction) return

        val normalizedNote = optimizationNote?.trim()
        if (targetStatus == CaseStatus.TAMAMLANDI && normalizedNote.isNullOrEmpty()) {
            _uiState.update { it.copy(actionErrorMessage = UiText.Resource(R.string.error_optimization_note_required)) }
            return
        }
        if (normalizedNote != null && (normalizedNote.length > 1000 || '<' in normalizedNote || '>' in normalizedNote)) {
            _uiState.update { it.copy(actionErrorMessage = UiText.Resource(R.string.error_invalid_optimization_note)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, actionErrorMessage = null) }
            when (val result = repository.changeCaseStatus(selectedCase.caseId, targetStatus, normalizedNote)) {
                is ExpertResult.Success -> _uiState.update { state ->
                    state.copy(
                        selectedCase = result.value,
                        cases = state.cases.map { current ->
                            if (current.caseId == result.value.caseId) result.value else current
                        },
                        isSubmittingAction = false
                    )
                }
                is ExpertResult.Failure -> _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        actionErrorMessage = errorMessage(result.error.code)
                    )
                }
            }
        }
    }

    fun clearActionError() = _uiState.update { it.copy(actionErrorMessage = null) }

    fun loadCampaigns(
        reset: Boolean = true,
        status: CampaignStatus? = _uiState.value.campaignStatusFilter,
        segment: Segment? = _uiState.value.campaignSegmentFilter
    ) {
        val current = _uiState.value
        if (current.isLoadingCampaigns || current.isLoadingNextCampaignPage) return
        if (!reset && !current.canLoadMoreCampaigns) return
        val requestedPage = if (reset) 0 else current.campaignPage + 1
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingCampaigns = reset,
                    isLoadingNextCampaignPage = !reset,
                    campaignErrorMessage = null,
                    campaignStatusFilter = status,
                    campaignSegmentFilter = segment,
                    campaigns = if (reset) emptyList() else it.campaigns
                )
            }
            when (val result = repository.getCampaigns(page = requestedPage, size = PAGE_SIZE, status = status, segment = segment)) {
                is ExpertResult.Success -> _uiState.update {
                    it.copy(
                        campaigns = if (reset) result.value.items else it.campaigns + result.value.items,
                        campaignPage = result.value.page,
                        campaignTotal = result.value.total,
                        isLoadingCampaigns = false,
                        isLoadingNextCampaignPage = false
                    )
                }
                is ExpertResult.Failure -> _uiState.update {
                    it.copy(
                        isLoadingCampaigns = false,
                        isLoadingNextCampaignPage = false,
                        campaignErrorMessage = campaignErrorMessage(result.error.code)
                    )
                }
            }
        }
    }

    fun loadCampaignDetail(campaignNo: String) {
        if (_uiState.value.isLoadingCampaignDetail) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(selectedCampaign = null, isLoadingCampaignDetail = true, campaignErrorMessage = null)
            }
            when (val result = repository.getCampaignDetail(campaignNo)) {
                is ExpertResult.Success -> _uiState.update {
                    it.copy(selectedCampaign = result.value, isLoadingCampaignDetail = false)
                }
                is ExpertResult.Failure -> _uiState.update {
                    it.copy(isLoadingCampaignDetail = false, campaignErrorMessage = campaignErrorMessage(result.error.code))
                }
            }
        }
    }

    fun createCampaign(
        title: String,
        type: CampaignType,
        targetSegment: Segment,
        discountRate: Int,
        validUntil: String
    ) {
        if (_uiState.value.isCreatingCampaign) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isCreatingCampaign = true, campaignActionError = null, createdCampaignNo = null)
            }
            when (val result = repository.createCampaign(title, type, targetSegment, discountRate, validUntil)) {
                is ExpertResult.Success -> _uiState.update { state ->
                    state.copy(
                        selectedCampaign = result.value,
                        isCreatingCampaign = false,
                        createdCampaignNo = result.value.campaignNo
                    )
                }.also { loadCampaigns(reset = true) }
                is ExpertResult.Failure -> _uiState.update {
                    it.copy(
                        isCreatingCampaign = false,
                        campaignActionError = if (result.error.code == "VALIDATION_ERROR") {
                            UiText.Resource(R.string.error_invalid_campaign)
                        } else campaignErrorMessage(result.error.code)
                    )
                }
            }
        }
    }

    fun clearCampaignFeedback() = _uiState.update {
        it.copy(campaignActionError = null, createdCampaignNo = null)
    }

    private fun errorMessage(code: String): UiText = UiText.Resource(when (code) {
        "NETWORK_ERROR" -> R.string.error_network
        "FORBIDDEN" -> R.string.error_case_forbidden
        "NOT_FOUND" -> R.string.error_case_not_found
        "INVALID_STATE_TRANSITION" -> R.string.error_invalid_state_transition
        "OPTIMIZATION_NOTE_REQUIRED" -> R.string.error_optimization_note_required
        "VALIDATION_ERROR" -> R.string.error_invalid_optimization_note
        else -> R.string.error_unknown
    })

    private fun campaignErrorMessage(code: String): UiText = UiText.Resource(when (code) {
        "NETWORK_ERROR" -> R.string.error_network
        "FORBIDDEN" -> R.string.error_forbidden
        "NOT_FOUND" -> R.string.error_campaign_not_found
        "VALIDATION_ERROR" -> R.string.error_invalid_campaign
        else -> R.string.error_unknown
    })

    class Factory(private val repository: ExpertRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ExpertViewModel(repository) as T
    }

    private companion object { const val PAGE_SIZE = 20 }
}
