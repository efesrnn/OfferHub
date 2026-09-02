package com.example.offerhub.screens.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import com.example.offerhub.R
import com.example.offerhub.components.ExpertCaseCard
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.OptimizationCase
import com.example.offerhub.data.model.campaign.Priority

private enum class CaseGroup { ACTIVE, COMPLETED }
private enum class ActiveStatusFilter { ALL, ASSIGNED, OPTIMIZING, TESTING }
private enum class PriorityFilter { ALL, CRITICAL, HIGH, MEDIUM, LOW }
private enum class CaseSort { PRIORITY, SLA, NEWEST, OLDEST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertCaseListScreen(
    cases: List<OptimizationCase>,
    isLoading: Boolean,
    isLoadingNextPage: Boolean,
    canLoadMore: Boolean,
    errorMessage: String?,
    initialCriticalOnly: Boolean,
    initialStatusFilter: CaseStatus?,
    onRetryClick: () -> Unit,
    onLoadNextPage: () -> Unit,
    onStatusFilterChanged: (CaseStatus?) -> Unit,
    onCaseClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedGroup by rememberSaveable { mutableStateOf(CaseGroup.ACTIVE) }
    var statusFilter by rememberSaveable(initialStatusFilter) {
        mutableStateOf(initialStatusFilter.toActiveStatusFilter())
    }
    var priorityFilter by rememberSaveable(initialCriticalOnly) {
        mutableStateOf(if (initialCriticalOnly) PriorityFilter.CRITICAL else PriorityFilter.ALL)
    }
    var selectedSort by rememberSaveable { mutableStateOf(CaseSort.PRIORITY) }
    var completedSearchQuery by rememberSaveable { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val activeFilterCount =
        (if (statusFilter == ActiveStatusFilter.ALL) 0 else 1) +
            (if (priorityFilter == PriorityFilter.ALL) 0 else 1)

    val visibleCases = remember(
        cases,
        selectedGroup,
        statusFilter,
        priorityFilter,
        selectedSort,
        completedSearchQuery
    ) {
        val normalizedSearchQuery = completedSearchQuery.trim()
        cases.asSequence()
            .filter { case ->
                when (selectedGroup) {
                    CaseGroup.ACTIVE -> case.status in activeStatuses
                    CaseGroup.COMPLETED -> case.status in completedStatuses
                }
            }
            .filter { case ->
                if (selectedGroup == CaseGroup.COMPLETED) true else when (statusFilter) {
                    ActiveStatusFilter.ALL -> true
                    ActiveStatusFilter.ASSIGNED -> case.status == CaseStatus.ATANDI
                    ActiveStatusFilter.OPTIMIZING -> case.status == CaseStatus.OPTIMIZE_EDILIYOR
                    ActiveStatusFilter.TESTING -> case.status == CaseStatus.TEST_EDILIYOR
                }
            }
            .filter { case ->
                selectedGroup == CaseGroup.COMPLETED || when (priorityFilter) {
                    PriorityFilter.ALL -> true
                    PriorityFilter.CRITICAL -> case.priority == Priority.KRITIK
                    PriorityFilter.HIGH -> case.priority == Priority.YUKSEK
                    PriorityFilter.MEDIUM -> case.priority == Priority.ORTA
                    PriorityFilter.LOW -> case.priority == Priority.DUSUK
                }
            }
            .filter { case ->
                selectedGroup != CaseGroup.COMPLETED ||
                    normalizedSearchQuery.isBlank() ||
                    case.campaignNo.contains(normalizedSearchQuery, ignoreCase = true)
            }
            .sortedWith(if (selectedGroup == CaseGroup.COMPLETED) caseComparator(CaseSort.NEWEST) else caseComparator(selectedSort))
            .toList()
    }

    val shouldLoadNextPage by remember(listState, canLoadMore, isLoadingNextPage) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            canLoadMore && !isLoadingNextPage && lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadNextPage, cases.size) {
        if (shouldLoadNextPage) onLoadNextPage()
    }

    Scaffold(
        topBar = {
            OfferHubDetailTopBar(
                title = stringResource(R.string.expert_assigned_cases),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            errorMessage != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetryClick, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.admin_try_again))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SingleChoiceSegmentedButtonRow(
                        Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        CaseGroup.entries.forEachIndexed { index, group ->
                            SegmentedButton(
                                selected = selectedGroup == group,
                                onClick = {
                                    selectedGroup = group
                                    if (group == CaseGroup.COMPLETED && statusFilter != ActiveStatusFilter.ALL) {
                                        statusFilter = ActiveStatusFilter.ALL
                                        onStatusFilterChanged(null)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, CaseGroup.entries.size)
                            ) {
                                Text(stringResource(if (group == CaseGroup.ACTIVE) R.string.expert_active_cases else R.string.expert_completed_cases))
                            }
                        }
                    }
                }
                if (selectedGroup == CaseGroup.ACTIVE) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = { showFilterSheet = true }) {
                                Text(
                                    if (activeFilterCount == 0) {
                                        stringResource(R.string.expert_filters)
                                    } else {
                                        stringResource(R.string.expert_filters_count, activeFilterCount)
                                    }
                                )
                            }
                            Box {
                                TextButton(onClick = { showSortOptions = true }) {
                                    Text(stringResource(R.string.expert_sort_by, sortLabel(selectedSort)))
                                }
                                DropdownMenu(
                                    expanded = showSortOptions,
                                    onDismissRequest = { showSortOptions = false }
                                ) {
                                    CaseSort.entries.forEach { sort ->
                                        DropdownMenuItem(
                                            text = { Text(sortLabel(sort)) },
                                            onClick = {
                                                selectedSort = sort
                                                showSortOptions = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = completedSearchQuery,
                            onValueChange = { completedSearchQuery = it },
                            label = { Text(stringResource(R.string.expert_search_completed_cases)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (completedSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { completedSearchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.admin_clear_search)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (visibleCases.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(
                                    when {
                                        selectedGroup == CaseGroup.COMPLETED && completedSearchQuery.isBlank() ->
                                            R.string.expert_no_completed_cases
                                        else -> R.string.expert_no_matching_cases
                                    }
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(
                                    when {
                                        selectedGroup == CaseGroup.COMPLETED && completedSearchQuery.isBlank() ->
                                            R.string.expert_no_completed_cases_description
                                        selectedGroup == CaseGroup.COMPLETED ->
                                            R.string.expert_try_different_completed_search
                                        else -> R.string.expert_adjust_filters
                                    }
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                } else {
                    items(visibleCases, key = { it.caseId }) { case ->
                        ExpertCaseCard(case, onClick = { onCaseClick(case.caseId) })
                    }
                }
                if (isLoadingNextPage) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        CaseFilterSheet(
            currentStatus = statusFilter,
            currentPriority = priorityFilter,
            onDismiss = { showFilterSheet = false },
            onApply = { status, priority ->
                statusFilter = status
                priorityFilter = priority
                onStatusFilterChanged(status.toCaseStatus())
                showFilterSheet = false
            }
        )
    }
}

private fun ActiveStatusFilter.toCaseStatus(): CaseStatus? = when (this) {
    ActiveStatusFilter.ALL -> null
    ActiveStatusFilter.ASSIGNED -> CaseStatus.ATANDI
    ActiveStatusFilter.OPTIMIZING -> CaseStatus.OPTIMIZE_EDILIYOR
    ActiveStatusFilter.TESTING -> CaseStatus.TEST_EDILIYOR
}

private fun CaseStatus?.toActiveStatusFilter(): ActiveStatusFilter = when (this) {
    CaseStatus.ATANDI -> ActiveStatusFilter.ASSIGNED
    CaseStatus.OPTIMIZE_EDILIYOR -> ActiveStatusFilter.OPTIMIZING
    CaseStatus.TEST_EDILIYOR -> ActiveStatusFilter.TESTING
    else -> ActiveStatusFilter.ALL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseFilterSheet(
    currentStatus: ActiveStatusFilter,
    currentPriority: PriorityFilter,
    onDismiss: () -> Unit,
    onApply: (ActiveStatusFilter, PriorityFilter) -> Unit
) {
    var draftStatus by remember(currentStatus) { mutableStateOf(currentStatus) }
    var draftPriority by remember(currentPriority) { mutableStateOf(currentPriority) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(R.string.expert_filter_cases),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.expert_status_filter), fontWeight = FontWeight.SemiBold)
            CompactChipRows(
                values = ActiveStatusFilter.entries,
                selected = draftStatus,
                label = { statusFilterLabel(it) },
                onSelect = { draftStatus = it }
            )
            Text(stringResource(R.string.expert_priority_filter), fontWeight = FontWeight.SemiBold)
            CompactChipRows(
                values = PriorityFilter.entries,
                selected = draftPriority,
                label = { priorityFilterLabel(it) },
                onSelect = { draftPriority = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        draftStatus = ActiveStatusFilter.ALL
                        draftPriority = PriorityFilter.ALL
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.admin_clear))
                }
                Button(
                    onClick = { onApply(draftStatus, draftPriority) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.admin_apply))
                }
            }
        }
    }
}

@Composable
private fun <T> CompactChipRows(
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.chunked(3).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        label = { Text(label(value)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun statusFilterLabel(filter: ActiveStatusFilter): String = stringResource(
    when (filter) {
        ActiveStatusFilter.ALL -> R.string.expert_filter_all
        ActiveStatusFilter.ASSIGNED -> R.string.expert_filter_assigned
        ActiveStatusFilter.OPTIMIZING -> R.string.expert_filter_optimizing
        ActiveStatusFilter.TESTING -> R.string.expert_filter_testing
    }
)

@Composable
private fun priorityFilterLabel(filter: PriorityFilter): String = stringResource(
    when (filter) {
        PriorityFilter.ALL -> R.string.expert_filter_all
        PriorityFilter.CRITICAL -> R.string.expert_priority_critical
        PriorityFilter.HIGH -> R.string.expert_priority_high
        PriorityFilter.MEDIUM -> R.string.expert_priority_medium
        PriorityFilter.LOW -> R.string.expert_priority_low
    }
)

@Composable
private fun sortLabel(sort: CaseSort): String = stringResource(
    when (sort) {
        CaseSort.PRIORITY -> R.string.expert_sort_priority
        CaseSort.SLA -> R.string.expert_sort_sla
        CaseSort.NEWEST -> R.string.expert_sort_newest
        CaseSort.OLDEST -> R.string.expert_sort_oldest
    }
)

private fun caseComparator(sort: CaseSort): Comparator<OptimizationCase> = when (sort) {
    CaseSort.PRIORITY -> compareBy<OptimizationCase> { priorityRank(it.priority) }
        .thenBy { it.slaRemainingSeconds ?: Long.MAX_VALUE }
    CaseSort.SLA -> compareBy { it.slaRemainingSeconds ?: Long.MAX_VALUE }
    CaseSort.NEWEST -> compareByDescending { it.createdAt }
    CaseSort.OLDEST -> compareBy { it.createdAt }
}

private fun priorityRank(priority: Priority): Int = when (priority) {
    Priority.KRITIK -> 0
    Priority.YUKSEK -> 1
    Priority.ORTA -> 2
    Priority.DUSUK -> 3
    Priority.UNKNOWN -> 4
}

private val activeStatuses = setOf(
    CaseStatus.ATANDI,
    CaseStatus.OPTIMIZE_EDILIYOR,
    CaseStatus.TEST_EDILIYOR
)

private val completedStatuses = setOf(
    CaseStatus.TAMAMLANDI,
    CaseStatus.YAYINDA,
    CaseStatus.ARSIVLENDI
)
