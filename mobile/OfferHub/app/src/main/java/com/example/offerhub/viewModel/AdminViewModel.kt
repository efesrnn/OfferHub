package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.data.network.ApiError
import com.example.offerhub.repository.AdminRepository
import com.example.offerhub.repository.AdminResult
import com.example.offerhub.ui.text.UiText
import com.example.offerhub.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val auditLogs: List<AuditLog> = emptyList(),
    val auditPage: Int = 0,
    val auditTotal: Long = 0,
    val actionQuery: String? = null,
    val auditAction: String? = null,
    val auditResult: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val isLoadingAudit: Boolean = false,
    val isLoadingNextAuditPage: Boolean = false,
    val auditError: String? = null,
    val auditNextPageError: String? = null,
    val isSubmitting: Boolean = false,
    val actionMessage: UiText? = null,
    val actionError: UiText? = null,
    val createdStaffId: String? = null,
    val selectedStaff: AdminStaff? = null,
    val staffSearchQuery: String = "",
    val staffSearchResults: List<AdminStaff> = emptyList(),
    val isSearchingStaff: Boolean = false,
    val staffSearchError: String? = null
) {
    val canLoadMoreAudit: Boolean
        get() = auditLogs.size < auditTotal
}

class AdminViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var auditJob: Job? = null
    private var staffSearchJob: Job? = null

    fun loadAuditLogs(reset: Boolean = true) {
        val current = _uiState.value
        if (reset) {
            auditJob?.cancel()
        } else if (current.isLoadingAudit || current.isLoadingNextAuditPage) {
            return
        }
        if (!reset && !current.canLoadMoreAudit) return

        val requestedPage = if (reset) 0 else current.auditPage + 1
        auditJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAudit = reset,
                    isLoadingNextAuditPage = !reset,
                    auditError = if (reset) null else it.auditError,
                    auditNextPageError = null,
                    auditLogs = if (reset) emptyList() else it.auditLogs
                )
            }
            when (val result = repository.getAuditLogs(
                actionQuery = _uiState.value.actionQuery,
                action = _uiState.value.auditAction,
                result = _uiState.value.auditResult,
                fromDate = _uiState.value.fromDate,
                toDate = _uiState.value.toDate,
                page = requestedPage,
                size = PAGE_SIZE
            )) {
                is AdminResult.Success -> _uiState.update {
                    it.copy(
                        auditLogs = if (reset) {
                            result.value.items.distinctBy(AuditLog::id)
                        } else {
                            (it.auditLogs + result.value.items).distinctBy(AuditLog::id)
                        },
                        auditPage = result.value.page,
                        auditTotal = result.value.total,
                        auditError = null,
                        auditNextPageError = null
                    )
                }
                is AdminResult.Failure -> _uiState.update {
                    val message = result.error.message ?: "Audit logs could not be loaded"
                    if (reset) {
                        it.copy(auditError = message)
                    } else {
                        it.copy(auditNextPageError = message)
                    }
                }
            }
            _uiState.update { it.copy(isLoadingAudit = false, isLoadingNextAuditPage = false) }
        }
    }

    fun onActionQueryChange(query: String?) {
        _uiState.update { it.copy(actionQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadAuditLogs(reset = true)
        }
    }

    fun applyAuditFilters(
        action: String?,
        result: String?,
        fromDate: String?,
        toDate: String?
    ) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                auditAction = action,
                auditResult = result,
                fromDate = fromDate,
                toDate = toDate
            )
        }
        loadAuditLogs(reset = true)
    }

    fun clearAuditFilters() = applyAuditFilters(null, null, null, null)

    fun createStaff(
        firstName: String,
        lastName: String,
        email: String,
        role: String,
        specialties: List<String>,
        regions: List<String>
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            beginSubmission()
            when (val result = repository.createStaff(firstName, lastName, email, role, specialties, regions)) {
                is AdminResult.Success -> _uiState.update {
                    it.copy(
                        actionMessage = UiText.Resource(R.string.admin_staff_created_success),
                        createdStaffId = result.value.id
                    )
                }
                is AdminResult.Failure -> _uiState.update {
                    it.copy(actionError = result.error.toUiText(R.string.admin_staff_create_failed))
                }
            }
            loadAuditLogs(reset = true)
            endSubmission()
        }
    }

    fun updateRole(staffId: String, role: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            beginSubmission()
            val activeQuery = _uiState.value.staffSearchQuery
            var shouldRefreshStaff = false
            when (val result = repository.updateRole(staffId, role)) {
                is AdminResult.Success -> _uiState.update {
                    shouldRefreshStaff = true
                    it.copy(
                        selectedStaff = null,
                        actionMessage = UiText.Resource(
                            R.string.admin_role_updated,
                            listOf(result.value.role)
                        )
                    )
                }
                is AdminResult.Failure -> _uiState.update {
                    it.copy(actionError = result.error.toUiText(R.string.admin_role_update_failed))
                }
            }
            if (shouldRefreshStaff) {
                searchStaff(query = activeQuery, useDebounce = false)
            }
            loadAuditLogs(reset = true)
            endSubmission()
        }
    }

    fun loadStaff() = searchStaff(query = "", useDebounce = false)

    fun selectStaff(staff: AdminStaff) {
        _uiState.update {
            it.copy(
                selectedStaff = staff,
                actionMessage = null,
                actionError = null
            )
        }
    }

    fun clearSelectedStaff() {
        _uiState.update {
            it.copy(
                selectedStaff = null,
                actionMessage = null,
                actionError = null
            )
        }
    }

    fun onStaffSearchQueryChange(query: String) {
        searchStaff(query = query, useDebounce = query.isNotBlank())
    }

    private fun searchStaff(query: String, useDebounce: Boolean) {
        staffSearchJob?.cancel()
        val normalizedQuery = query.trim()
        _uiState.update {
            it.copy(
                staffSearchQuery = query,
                staffSearchResults = emptyList(),
                selectedStaff = null,
                isSearchingStaff = true,
                staffSearchError = null
            )
        }

        staffSearchJob = viewModelScope.launch {
            if (useDebounce) delay(STAFF_SEARCH_DEBOUNCE_MS)
            when (val result = repository.searchStaff(normalizedQuery)) {
                is AdminResult.Success -> _uiState.update {
                    if (it.staffSearchQuery.trim() == normalizedQuery) {
                        it.copy(
                            staffSearchResults = result.value,
                            isSearchingStaff = false,
                            staffSearchError = null
                        )
                    } else it
                }
                is AdminResult.Failure -> _uiState.update {
                    if (it.staffSearchQuery.trim() == normalizedQuery) {
                        it.copy(
                            staffSearchResults = emptyList(),
                            isSearchingStaff = false,
                            staffSearchError = result.error.message
                        )
                    } else it
                }
            }
        }
    }

    fun clearStaffSearch() {
        searchStaff(query = "", useDebounce = false)
    }

    fun clearActionFeedback() {
        _uiState.update { it.copy(actionMessage = null, actionError = null, createdStaffId = null) }
    }

    private fun beginSubmission() = _uiState.update {
        it.copy(
            isSubmitting = true,
            actionMessage = null,
            actionError = null,
            createdStaffId = null
        )
    }

    private fun endSubmission() = _uiState.update { it.copy(isSubmitting = false) }

    private fun ApiError.toUiText(fallbackResource: Int): UiText =
        message?.takeIf { it.isNotBlank() }
            ?.let(UiText::Dynamic)
            ?: UiText.Resource(fallbackResource)

    class Factory(private val repository: AdminRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminViewModel(repository) as T
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 400L
        const val STAFF_SEARCH_DEBOUNCE_MS = 400L
    }
}
