package com.example.offerhub.screens.supervisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.data.model.supervisor.SupervisorCaseSummary
import com.example.offerhub.data.model.supervisor.ExpertPerformanceSummary
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.CaseStatus

enum class SupervisorCaseListMode { PENDING_ASSIGNMENT, ACTIVE, APPROVAL, PUBLISHED }
private enum class ActiveCaseTab(val status: CaseStatus) {
    ASSIGNED(CaseStatus.ATANDI),
    OPTIMIZING(CaseStatus.OPTIMIZE_EDILIYOR),
    TESTING(CaseStatus.TEST_EDILIYOR)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SupervisorCaseListScreen(
    title: String,
    cases: List<SupervisorCaseSummary>,
    mode: SupervisorCaseListMode,
    experts: List<ExpertPerformanceSummary>,
    isLoading: Boolean,
    loadError: String?,
    isSubmitting: Boolean,
    actionError: String?,
    onAssignCase: (String, String) -> Unit,
    onPublishCase: (String) -> Unit,
    onUpdateClassification: (String, Segment, Priority) -> Unit,
    onClearActionError: () -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var selectedCase by remember { mutableStateOf<SupervisorCaseSummary?>(null) }
    var editingCase by remember { mutableStateOf<SupervisorCaseSummary?>(null) }
    var assignmentCase by remember { mutableStateOf<SupervisorCaseSummary?>(null) }
    var assignmentQuery by remember { mutableStateOf("") }
    var pendingClassification by remember { mutableStateOf<Triple<String, Segment, Priority>?>(null) }
    var activeCaseTab by remember { mutableStateOf(ActiveCaseTab.ASSIGNED) }
    val displayedCases = if (mode == SupervisorCaseListMode.ACTIVE) {
        cases.filter { it.status == activeCaseTab.status }
    } else {
        cases
    }
    LaunchedEffect(cases, selectedCase?.caseId) {
        selectedCase?.let { selected ->
            selectedCase = cases.firstOrNull { it.caseId == selected.caseId }
        }
    }
    Scaffold(
        topBar = { OfferHubDetailTopBar(title, onBackClick) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (mode == SupervisorCaseListMode.ACTIVE) {
                item {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ActiveCaseTab.entries.forEachIndexed { index, tab ->
                            val count = cases.count { it.status == tab.status }
                            SegmentedButton(
                                selected = activeCaseTab == tab,
                                onClick = { activeCaseTab = tab },
                                shape = SegmentedButtonDefaults.itemShape(index, ActiveCaseTab.entries.size)
                            ) {
                                Text(tab.label(count))
                            }
                        }
                    }
                }
            }
            if (isLoading && cases.isEmpty()) {
                item {
                    Row(Modifier.fillParentMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (loadError != null && cases.isEmpty()) {
                item {
                    Column(
                        Modifier.fillParentMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(loadError, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetryClick) { Text(stringResource(R.string.admin_try_again)) }
                    }
                }
            } else if (displayedCases.isEmpty()) {
                item {
                    Text(
                        mode.emptyMessage(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(displayedCases, key = { it.caseId }) { item ->
                val canEdit = item.status in setOf(CaseStatus.YENI, CaseStatus.ATANDI, CaseStatus.OPTIMIZE_EDILIYOR)
                val hasBreachedSla = item.slaRemainingSeconds != null && item.slaRemainingSeconds < 0
                Row(
                    modifier = Modifier.fillParentMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        onClick = { selectedCase = item },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasBreachedSla) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text(item.priority.displayName())
                            Text(item.assignedExpertId ?: stringResource(R.string.supervisor_waiting_assignment))
                            Text(
                                stringResource(R.string.supervisor_sla_card_value, item.slaRemainingSeconds.toSlaText()),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasBreachedSla) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (hasBreachedSla) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (canEdit) {
                            CompactActionButton(stringResource(R.string.supervisor_edit)) { editingCase = item }
                        }
                        if (
                            mode == SupervisorCaseListMode.PENDING_ASSIGNMENT &&
                            item.status == CaseStatus.YENI &&
                            item.assignedExpertId == null
                        ) {
                            CompactActionButton(stringResource(R.string.supervisor_assign)) {
                                assignmentCase = item
                                assignmentQuery = ""
                            }
                        }
                    }
                }
            }
        }
    }
    selectedCase?.let { item ->
        ModalBottomSheet(onDismissRequest = { selectedCase = null; onClearActionError() }) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                CaseDetailRow(stringResource(R.string.supervisor_case_id), item.caseId)
                CaseDetailRow(stringResource(R.string.supervisor_priority), item.priority.displayName())
                CaseDetailRow(stringResource(R.string.supervisor_status), item.status.displayName())
                CaseDetailRow(stringResource(R.string.supervisor_segment), item.segment.displayName())
                CaseDetailRow(
                    stringResource(R.string.supervisor_assigned_expert),
                    item.assignedExpertId ?: stringResource(R.string.supervisor_waiting_assignment)
                )
                CaseDetailRow(stringResource(R.string.supervisor_sla_remaining), item.slaRemainingSeconds.toSlaText())
                if (mode == SupervisorCaseListMode.APPROVAL) {
                    Button(
                        onClick = { onPublishCase(item.caseId) },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.supervisor_publish_case)) }
                }
                actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(
                    onClick = { selectedCase = null; onClearActionError() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.admin_close)) }
            }
        }
    }
    editingCase?.let { item ->
        var selectedSegment by remember(item.caseId, item.segment) { mutableStateOf(item.segment) }
        var selectedPriority by remember(item.caseId, item.priority) { mutableStateOf(item.priority) }
        ModalBottomSheet(onDismissRequest = { editingCase = null; onClearActionError() }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(stringResource(R.string.supervisor_update_classification), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(item.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.supervisor_segment), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Segment.entries.filter { it != Segment.UNKNOWN }) { segment ->
                        FilterChip(
                            selectedSegment == segment,
                            {
                                selectedSegment = segment
                                if (segment == Segment.RISKLI_KAYIP && selectedPriority !in setOf(Priority.YUKSEK, Priority.KRITIK)) {
                                    selectedPriority = Priority.YUKSEK
                                }
                            },
                            { Text(segment.displayName()) }
                        )
                    }
                }
                Text(stringResource(R.string.supervisor_priority), style = MaterialTheme.typography.labelMedium)
                val priorities = if (selectedSegment == Segment.RISKLI_KAYIP) {
                    listOf(Priority.YUKSEK, Priority.KRITIK)
                } else {
                    Priority.entries.filter { it != Priority.UNKNOWN }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(priorities) { priority ->
                        FilterChip(selectedPriority == priority, { selectedPriority = priority }, { Text(priority.displayName()) })
                    }
                }
                Button(
                    onClick = {
                            if (item.status == CaseStatus.OPTIMIZE_EDILIYOR) {
                                pendingClassification = Triple(item.caseId, selectedSegment, selectedPriority)
                            } else {
                                editingCase = null
                                onUpdateClassification(item.caseId, selectedSegment, selectedPriority)
                            }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.supervisor_save_changes)) }
                actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(
                    onClick = { editingCase = null; onClearActionError() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.admin_close)) }
            }
        }
    }
    assignmentCase?.let { item ->
        val filteredExperts = experts.filter { it.displayName.contains(assignmentQuery.trim(), ignoreCase = true) }
        ModalBottomSheet(onDismissRequest = { assignmentCase = null; onClearActionError() }) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.supervisor_assign_expert), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = assignmentQuery,
                    onValueChange = { assignmentQuery = it },
                    label = { Text(stringResource(R.string.supervisor_search_expert)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                filteredExperts.forEach { expert ->
                    val hasCapacity = expert.activeCaseCount < expert.maximumCaseCapacity
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(expert.displayName, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.supervisor_capacity_value, expert.activeCaseCount, expert.maximumCaseCapacity))
                            }
                            Button(
                                onClick = { assignmentCase = null; onAssignCase(item.caseId, expert.expertId) },
                                enabled = !isSubmitting && hasCapacity
                            ) { Text(stringResource(R.string.supervisor_assign)) }
                        }
                    }
                }
                if (filteredExperts.isEmpty()) Text(stringResource(R.string.supervisor_no_experts))
                actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(
                    onClick = { assignmentCase = null; onClearActionError() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.admin_close)) }
            }
        }
    }
    pendingClassification?.let { (caseId, segment, priority) ->
        AlertDialog(
            onDismissRequest = { pendingClassification = null },
            title = { Text(stringResource(R.string.supervisor_confirm_active_edit_title)) },
            text = { Text(stringResource(R.string.supervisor_confirm_active_edit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingClassification = null
                    editingCase = null
                    onUpdateClassification(caseId, segment, priority)
                }) { Text(stringResource(R.string.supervisor_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingClassification = null }) { Text(stringResource(R.string.supervisor_cancel)) }
            }
        )
    }
}

@Composable
private fun SupervisorCaseListMode.emptyMessage(): String = stringResource(
    when (this) {
        SupervisorCaseListMode.PENDING_ASSIGNMENT -> R.string.supervisor_empty_pending_cases
        SupervisorCaseListMode.ACTIVE -> R.string.supervisor_empty_active_cases
        SupervisorCaseListMode.APPROVAL -> R.string.supervisor_empty_approval_cases
        SupervisorCaseListMode.PUBLISHED -> R.string.supervisor_empty_published_cases
    }
)

@Composable
private fun ActiveCaseTab.label(count: Int): String = stringResource(
    when (this) {
        ActiveCaseTab.ASSIGNED -> R.string.supervisor_active_tab_assigned
        ActiveCaseTab.OPTIMIZING -> R.string.supervisor_active_tab_optimizing
        ActiveCaseTab.TESTING -> R.string.supervisor_active_tab_testing
    },
    count
)

@Composable
private fun CompactActionButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(78.dp).height(36.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CaseDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Long?.toSlaText(): String = when {
    this == null -> stringResource(R.string.supervisor_sla_unknown)
    this < 0 -> stringResource(R.string.supervisor_sla_exceeded)
    else -> "%02d:%02d".format(this / 3600, (this % 3600) / 60)
}

@Composable
private fun Priority.displayName(): String = stringResource(
    when (this) {
        Priority.DUSUK -> R.string.expert_priority_low
        Priority.ORTA -> R.string.expert_priority_medium
        Priority.YUKSEK -> R.string.expert_priority_high
        Priority.KRITIK -> R.string.expert_priority_critical
        Priority.UNKNOWN -> R.string.common_not_available
    }
)

@Composable
private fun Segment.displayName(): String = stringResource(
    when (this) {
        Segment.YUKSEK_DEGER -> R.string.supervisor_segment_high_value
        Segment.RISKLI_KAYIP -> R.string.supervisor_segment_churn_risk
        Segment.YENI_ABONE -> R.string.supervisor_segment_new_subscriber
        Segment.PASIF -> R.string.supervisor_segment_inactive
        Segment.BELIRSIZ -> R.string.supervisor_segment_uncertain
        Segment.UNKNOWN -> R.string.common_not_available
    }
)

@Composable
private fun CaseStatus.displayName(): String = stringResource(
    when (this) {
        CaseStatus.YENI -> R.string.supervisor_status_new
        CaseStatus.ATANDI -> R.string.supervisor_status_assigned
        CaseStatus.OPTIMIZE_EDILIYOR -> R.string.supervisor_status_optimizing
        CaseStatus.TEST_EDILIYOR -> R.string.supervisor_status_testing
        CaseStatus.TAMAMLANDI -> R.string.supervisor_status_completed
        CaseStatus.YAYINDA -> R.string.supervisor_status_published
        CaseStatus.ARSIVLENDI -> R.string.supervisor_status_archived
        CaseStatus.UNKNOWN -> R.string.common_not_available
    }
)
