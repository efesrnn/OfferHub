package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.data.model.Offer
import com.example.offerhub.repository.SubscriberRepository
import com.example.offerhub.repository.SubscriberResult
import com.example.offerhub.R
import com.example.offerhub.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriberUiState(
    val isLoading: Boolean = false,
    val offers: List<Offer> = emptyList(),
    val selectedOffer: Offer? = null,
    val loadErrorMessage: UiText? = null,
    val actionErrorMessage: UiText? = null,
    val isSubmittingAction: Boolean = false
)
class SubscriberViewModel(
    private val repository: SubscriberRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(SubscriberUiState())

    val uiState: StateFlow<SubscriberUiState> =
        _uiState.asStateFlow()

    init {
        loadOffers()
    }

    fun loadOffers() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }
            when (val result = repository.getOffers()) {
                is SubscriberResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        offers = result.value.sortedByDescending(Offer::score)
                    )
                }
                is SubscriberResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadErrorMessage = errorMessage(result.error.code)
                    )
                }
            }
        }
    }

    fun selectOffer(offerId: String) {
        _uiState.update { state ->
            state.copy(
                selectedOffer =
                    state.offers.firstOrNull {
                        it.offerId == offerId
                    }
            )
        }
    }

    fun dismissOfferDetail() {
        _uiState.update {
            it.copy(selectedOffer = null, actionErrorMessage = null)
        }
    }

    fun acceptOffer(offerId: String) = executeAction {
        repository.acceptOffer(offerId)
    }

    fun declineOffer(offerId: String) = executeAction(closeDetail = true) {
        repository.declineOffer(offerId)
    }

    fun rateOffer(offerId: String, rating: Int) =
        executeAction(closeDetail = true) {
            repository.rateOffer(offerId, rating)
        }

    private fun executeAction(
        closeDetail: Boolean = false,
        operation: suspend () -> SubscriberResult<Offer>
    ) {
        if (_uiState.value.isSubmittingAction) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmittingAction = true, actionErrorMessage = null)
            }
            when (val result = operation()) {
                is SubscriberResult.Success -> _uiState.update { state ->
                    state.copy(
                        isSubmittingAction = false,
                        offers = state.offers
                            .map { if (it.offerId == result.value.offerId) result.value else it }
                            .sortedByDescending(Offer::score),
                        selectedOffer = if (closeDetail) null else result.value
                    )
                }
                is SubscriberResult.Failure -> _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        actionErrorMessage = errorMessage(result.error.code)
                    )
                }
            }
        }
    }

    private fun errorMessage(code: String): UiText = UiText.Resource(when (code) {
        "NETWORK_ERROR" -> R.string.error_network
        "OFFER_NOT_FOUND" -> R.string.error_offer_not_found
        "OFFER_NOT_ACCEPTED" -> R.string.error_offer_not_accepted
        "OFFER_ALREADY_RESPONDED" -> R.string.error_offer_already_responded
        "OFFER_ALREADY_RATED" -> R.string.error_offer_already_rated
        "INVALID_RATING" -> R.string.error_invalid_rating
        else -> R.string.error_unknown
    })

    class Factory(
        private val repository: SubscriberRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SubscriberViewModel(repository) as T
    }
}
