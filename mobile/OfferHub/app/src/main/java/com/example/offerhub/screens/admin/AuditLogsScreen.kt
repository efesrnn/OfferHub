package com.example.offerhub.screens.admin

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.R
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(
    logs: List<AuditLog>,
    actionQuery: String = "",
    selectedAction: String? = null,
    selectedResult: String? = null,
    selectedFromDate: String? = null,
    selectedToDate: String? = null,
    onBackClick: () -> Unit,
    onActionQueryChange: (String?) -> Unit,
    onApplyFilters: (String?, String?, String?, String?) -> Unit,
    onClearFilters: () -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryClick: () -> Unit,
    onRetryNextPageClick: () -> Unit,
    isLoading: Boolean = false,
    isLoadingNextPage: Boolean = false,
    canLoadMore: Boolean = false,
    errorMessage: String? = null,
    nextPageErrorMessage: String? = null
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectingFromDate by remember { mutableStateOf(true) }
    var draftAction by remember { mutableStateOf(selectedAction) }
    var draftResult by remember { mutableStateOf(selectedResult) }
    var draftFromDate by remember { mutableStateOf(selectedFromDate) }
    var draftToDate by remember { mutableStateOf(selectedToDate) }
    var selectedLog by remember { mutableStateOf<AuditLog?>(null) }
    val datePickerState = androidx.compose.material3.rememberDatePickerState()
    val hasInvalidDateRange = draftFromDate != null && draftToDate != null && draftFromDate!! > draftToDate!!
    val hasActiveFilters = selectedAction != null || selectedResult != null ||
        selectedFromDate != null || selectedToDate != null
    val listState = rememberLazyListState()
    val shouldLoadNextPage by remember(
        listState,
        canLoadMore,
        isLoadingNextPage,
        nextPageErrorMessage
    ) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            val totalItems = listState.layoutInfo.totalItemsCount
            canLoadMore && !isLoadingNextPage && nextPageErrorMessage == null &&
                totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadNextPage, logs.size) {
        if (shouldLoadNextPage) onLoadNextPage()
    }

    Scaffold(
        topBar = {
            OfferHubDetailTopBar(
                title = stringResource(R.string.admin_audit_logs),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = actionQuery,
                    onValueChange = {
                        onActionQueryChange(it.ifBlank { null })
                    },
                    label = { Text(stringResource(R.string.admin_search_action)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedIconButton(
                    onClick = {
                        draftAction = selectedAction
                        draftResult = selectedResult
                        draftFromDate = selectedFromDate
                        draftToDate = selectedToDate
                        showFilterSheet = true
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.admin_filters),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (hasActiveFilters) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedAction?.let { action ->
                        item(key = "action") {
                            FilterChip(
                                selected = true,
                                onClick = { onApplyFilters(null, selectedResult, selectedFromDate, selectedToDate) },
                                label = { Text(action) }
                            )
                        }
                    }
                    selectedResult?.let { result ->
                        item(key = "result") {
                            FilterChip(
                                selected = true,
                                onClick = { onApplyFilters(selectedAction, null, selectedFromDate, selectedToDate) },
                                label = { Text(result) }
                            )
                        }
                    }
                    selectedFromDate?.let { date ->
                        item(key = "from") {
                            FilterChip(
                                selected = true,
                                onClick = { onApplyFilters(selectedAction, selectedResult, null, selectedToDate) },
                                label = { Text(stringResource(R.string.admin_from_value, date)) }
                            )
                        }
                    }
                    selectedToDate?.let { date ->
                        item(key = "to") {
                            FilterChip(
                                selected = true,
                                onClick = { onApplyFilters(selectedAction, selectedResult, selectedFromDate, null) },
                                label = { Text(stringResource(R.string.admin_to_value, date)) }
                            )
                        }
                    }
                }
            }

            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                errorMessage != null -> {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetryClick) { Text(stringResource(R.string.admin_try_again)) }
                }
                logs.isEmpty() -> Text(stringResource(R.string.admin_no_audit_logs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        AuditLogCard(log = log, onClick = { selectedLog = log })
                    }
                    if (isLoadingNextPage) {
                        item(key = "next-page-loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (nextPageErrorMessage != null) {
                        item(key = "next-page-error") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.admin_next_page_error),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = onRetryNextPageClick) {
                                    Text(stringResource(R.string.admin_retry_more_logs))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                            ?.let { millis ->
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString()
                            }
                        if (selectingFromDate) draftFromDate = selectedDate else draftToDate = selectedDate
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.admin_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.admin_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.admin_filter_audit_logs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(R.string.admin_action_type), fontWeight = FontWeight.SemiBold)
                AuditFilterChoices(
                    options = listOf("STAFF_CREATED", "ROLE_UPDATED", "LOGIN_SUCCESS", "LOGIN_FAILED"),
                    selected = draftAction,
                    onSelect = { draftAction = it }
                )
                Text(stringResource(R.string.admin_result), fontWeight = FontWeight.SemiBold)
                AuditFilterChoices(
                    options = listOf("SUCCESS", "FAILED"),
                    selected = draftResult,
                    onSelect = { draftResult = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectingFromDate = true
                            showDatePicker = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(draftFromDate ?: stringResource(R.string.admin_from_date))
                    }
                    OutlinedButton(
                        onClick = {
                            selectingFromDate = false
                            showDatePicker = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(draftToDate ?: stringResource(R.string.admin_to_date))
                    }
                }
                if (hasInvalidDateRange) {
                    Text(
                        stringResource(R.string.admin_invalid_date_range),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            draftAction = null
                            draftResult = null
                            draftFromDate = null
                            draftToDate = null
                            onClearFilters()
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.admin_clear_filters)) }
                    Button(
                        enabled = !hasInvalidDateRange,
                        onClick = {
                            onApplyFilters(draftAction, draftResult, draftFromDate, draftToDate)
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.admin_apply)) }
                }
            }
        }
    }

    selectedLog?.let { log ->
        AuditLogDetailSheet(
            log = log,
            onDismiss = { selectedLog = null }
        )
    }
}

@Composable
private fun AuditFilterChoices(
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(if (selected == option) null else option) },
                        label = { Text(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(
    log: AuditLog,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(log.action, fontWeight = FontWeight.Bold)
                Text(log.result, color = if (log.result == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Text(formatAuditTimestamp(log.timestamp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuditLogDetailSheet(
    log: AuditLog,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.admin_audit_details),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            AuditDetailRow(stringResource(R.string.admin_log_id), log.id, copyable = true)
            AuditDetailRow(stringResource(R.string.admin_user_id), log.userId, copyable = true)
            AuditDetailRow(stringResource(R.string.admin_action), log.action)
            AuditDetailRow(stringResource(R.string.admin_result), log.result)
            AuditDetailRow(stringResource(R.string.admin_timestamp), formatAuditTimestamp(log.timestamp))
            AuditDetailRow(stringResource(R.string.admin_ip_address), log.ip)
            log.detail?.takeIf { it.isNotBlank() }?.let {
                AuditDetailRow(stringResource(R.string.admin_detail), it)
            }
        }
    }
}

@Composable
private fun AuditDetailRow(label: String, value: String, copyable: Boolean = false) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (copyable) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText("audit log id", value))
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.admin_copy_id),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatAuditTimestamp(value: String): String = runCatching {
    DateTimeFormatter
        .ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
        .format(Instant.parse(value).atZone(ZoneId.systemDefault()))
}.getOrDefault(value)
