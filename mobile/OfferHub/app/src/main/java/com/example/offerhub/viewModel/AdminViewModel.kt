package com.example.offerhub.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.model.admin.AdminStaff
import com.example.offerhub.repository.AdminRepository
import com.example.offerhub.repository.AdminResult
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
    val isSubmitting: Boolean = false,
    val actionMessage: String? = null,
    val actionError: String? = null,
    val createdStaffId: String? = null,
    val staffIdQuery: String = "",
    val selectedStaff: AdminStaff? = null,
    val isLookingUpStaff: Boolean = false,
    val staffLookupError: String? = null,
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
    private var staffLookupJob: Job? = null
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
                    auditError = null,
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
                        auditLogs = if (reset) result.value.items else it.auditLogs + result.value.items,
                        auditPage = result.value.page,
                        auditTotal = result.value.total
                    )
                }
                is AdminResult.Failure -> _uiState.update {
                    it.copy(auditError = result.error.message ?: "Audit logs could not be loaded")
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
                        actionMessage = "Staff created successfully",
                        createdStaffId = result.value.id
                    )
                }
                is AdminResult.Failure -> _uiState.update {
                    it.copy(actionError = result.error.message ?: "Staff account could not be created")
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
            when (val result = repository.updateRole(staffId, role)) {
                is AdminResult.Success -> _uiState.update {
                    it.copy(
                        selectedStaff = result.value,
                        actionMessage = "Role updated to ${result.value.role}"
                    )
                }
                is AdminResult.Failure -> _uiState.update {
                    it.copy(actionError = result.error.message ?: "Role could not be updated")
                }
            }
            loadAuditLogs(reset = true)
            endSubmission()
        }
    }

    fun onStaffIdChange(staffId: String) {
        val normalizedStaffId = staffId.trim()
        staffLookupJob?.cancel()
        _uiState.update {
            it.copy(
                staffIdQuery = normalizedStaffId,
                selectedStaff = null,
                isLookingUpStaff = false,
                staffLookupError = null,
                actionMessage = null,
                actionError = null
            )
        }
        if (normalizedStaffId.isBlank()) return

        staffLookupJob = viewModelScope.launch {
            delay(STAFF_LOOKUP_DEBOUNCE_MS)
            _uiState.update { it.copy(isLookingUpStaff = true) }
            when (val result = repository.findStaff(normalizedStaffId)) {
                is AdminResult.Success -> _uiState.update {
                    if (it.staffIdQuery == normalizedStaffId) {
                        it.copy(selectedStaff = result.value, staffLookupError = null)
                    } else it
                }
                is AdminResult.Failure -> _uiState.update {
                    if (it.staffIdQuery == normalizedStaffId) {
                        it.copy(staffLookupError = result.error.message ?: "Staff member not found")
                    } else it
                }
            }
            _uiState.update { it.copy(isLookingUpStaff = false) }
        }
    }

    fun clearStaffLookup() {
        staffLookupJob?.cancel()
        _uiState.update {
            it.copy(
                staffIdQuery = "",
                selectedStaff = null,
                isLookingUpStaff = false,
                staffLookupError = null,
                actionMessage = null,
                actionError = null
            )
        }
    }

    fun onStaffSearchQueryChange(query: String) {
        staffSearchJob?.cancel()
        _uiState.update {
            it.copy(
                staffSearchQuery = query,
                staffSearchResults = if (query.isBlank()) emptyList() else it.staffSearchResults,
                isSearchingStaff = false,
                staffSearchError = null
            )
        }
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return

        staffSearchJob = viewModelScope.launch {
            delay(STAFF_SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearchingStaff = true) }
            when (val result = repository.searchStaff(normalizedQuery)) {
                is AdminResult.Success -> _uiState.update {
                    if (it.staffSearchQuery.trim() == normalizedQuery) {
                        it.copy(staffSearchResults = result.value, staffSearchError = null)
                    } else it
                }
                is AdminResult.Failure -> _uiState.update {
                    if (it.staffSearchQuery.trim() == normalizedQuery) {
                        it.copy(staffSearchResults = emptyList(), staffSearchError = result.error.message)
                    } else it
                }
            }
            _uiState.update { it.copy(isSearchingStaff = false) }
        }
    }

    fun clearStaffSearch() {
        staffSearchJob?.cancel()
        _uiState.update {
            it.copy(
                staffSearchQuery = "",
                staffSearchResults = emptyList(),
                isSearchingStaff = false,
                staffSearchError = null
            )
        }
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

    class Factory(private val repository: AdminRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminViewModel(repository) as T
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 400L
        const val STAFF_LOOKUP_DEBOUNCE_MS = 400L
        const val STAFF_SEARCH_DEBOUNCE_MS = 400L
    }
}
