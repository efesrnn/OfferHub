package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.R
import com.example.offerhub.data.model.gamification.GamificationProfile
import com.example.offerhub.data.model.gamification.RankingEntry
import com.example.offerhub.data.model.gamification.RankingPeriod
import com.example.offerhub.repository.GamificationRepository
import com.example.offerhub.repository.GamificationResult
import com.example.offerhub.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class GamificationUiState(
    val expertId: String = "",
    val profile: GamificationProfile? = null,
    val ranking: List<RankingEntry> = emptyList(),
    val selectedPeriod: RankingPeriod = RankingPeriod.DAILY,
    val isLoading: Boolean = false,
    val isLoadingRanking: Boolean = false,
    val errorMessage: UiText? = null
)

class GamificationViewModel(
    private val repository: GamificationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()
    private var rankingJob: Job? = null

    fun load(expertId: String) {
        if (expertId.isBlank() || _uiState.value.isLoading) return

        viewModelScope.launch(dispatcher) {
            val userChanged = _uiState.value.expertId != expertId
            _uiState.update {
                it.copy(
                    expertId = expertId,
                    profile = if (userChanged) null else it.profile,
                    ranking = if (userChanged) emptyList() else it.ranking,
                    isLoading = true,
                    errorMessage = null
                )
            }
            when (val result = repository.getProfile(expertId)) {
                is GamificationResult.Success -> {
                    _uiState.update { it.copy(profile = result.value, isLoading = false) }
                    loadRanking(_uiState.value.selectedPeriod)
                }
                is GamificationResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = errorText(result.error.code))
                }
            }
        }
    }

    fun selectPeriod(period: RankingPeriod) {
        if (period == _uiState.value.selectedPeriod && _uiState.value.ranking.isNotEmpty()) return
        _uiState.update { it.copy(selectedPeriod = period) }
        loadRanking(period)
    }

    fun retry() {
        val expertId = _uiState.value.expertId
        if (_uiState.value.profile == null) load(expertId) else loadRanking(_uiState.value.selectedPeriod)
    }

    private fun loadRanking(period: RankingPeriod) {
        val expertId = _uiState.value.expertId
        if (expertId.isBlank() || _uiState.value.isLoadingRanking) return

        rankingJob?.cancel()
        rankingJob = viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(isLoadingRanking = true, errorMessage = null) }
            when (val result = repository.getRanking(period, expertId)) {
                is GamificationResult.Success -> _uiState.update {
                    it.copy(ranking = result.value, isLoadingRanking = false)
                }
                is GamificationResult.Failure -> _uiState.update {
                    it.copy(isLoadingRanking = false, errorMessage = errorText(result.error.code))
                }
            }
        }
    }

    private fun errorText(code: String): UiText = UiText.Resource(
        if (code == "NETWORK_ERROR") R.string.error_network else R.string.error_gamification
    )

    class Factory(
        private val repository: GamificationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GamificationViewModel(repository) as T
    }
}
