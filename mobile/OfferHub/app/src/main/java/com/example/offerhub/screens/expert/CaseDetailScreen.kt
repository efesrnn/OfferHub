package com.example.offerhub.screens.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.OptimizationCase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertCaseDetailScreen(
    optimizationCase: OptimizationCase?,
    isLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    isNotFound: Boolean,
    actionErrorMessage: String?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onChangeStatus: (CaseStatus, String?) -> Unit,
    onClearActionError: () -> Unit
) {
    var showCompletionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(optimizationCase?.status) {
        if (optimizationCase?.status == CaseStatus.TAMAMLANDI) showCompletionSheet = false
    }

    Scaffold(
        topBar = {
            OfferHubDetailTopBar(
                title = stringResource(R.string.expert_case_detail),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        when {
            isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            errorMessage != null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetryClick, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(if (isNotFound) R.string.expert_back_to_cases else R.string.admin_try_again))
                }
            }

            optimizationCase != null -> CaseDetailContent(
                optimizationCase = optimizationCase,
                isSubmitting = isSubmitting,
                actionErrorMessage = actionErrorMessage,
                onChangeStatus = { targetStatus -> onChangeStatus(targetStatus, null) },
                onCompleteClick = {
                    onClearActionError()
                    showCompletionSheet = true
                },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showCompletionSheet && optimizationCase != null) {
        OptimizationNoteSheet(
            isSubmitting = isSubmitting,
            backendError = actionErrorMessage,
            onDismiss = {
                onClearActionError()
                showCompletionSheet = false
            },
            onSubmit = { note -> onChangeStatus(CaseStatus.TAMAMLANDI, note) }
        )
    }
}

@Composable
private fun CaseDetailContent(
    optimizationCase: OptimizationCase,
    isSubmitting: Boolean,
    actionErrorMessage: String?,
    onChangeStatus: (CaseStatus) -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(optimizationCase.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        DetailRow(stringResource(R.string.expert_campaign_number), optimizationCase.campaignNo)
        DetailRow(stringResource(R.string.expert_status), optimizationCase.status.displayName())
        DetailRow(stringResource(R.string.expert_priority), optimizationCase.priority.name.toDisplayText())
        DetailRow(stringResource(R.string.expert_sla), optimizationCase.slaRemainingSeconds.toSlaText())

        Text(stringResource(R.string.expert_ai_analysis), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        DetailRow(stringResource(R.string.expert_current_segment), optimizationCase.segment.name.toDisplayText())
        DetailRow(stringResource(R.string.expert_ai_segment), optimizationCase.aiSegment.name.toDisplayText())
        DetailRow(stringResource(R.string.expert_conversion_probability), optimizationCase.conversionProbability.toPercentage())
        DetailRow(stringResource(R.string.expert_recommendation_score), optimizationCase.recommendationScore?.let { "%.2f".format(it) } ?: stringResource(R.string.common_not_available))

        Text(stringResource(R.string.expert_case_information), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        DetailRow(stringResource(R.string.expert_assigned_expert), optimizationCase.assignedExpertId ?: stringResource(R.string.common_not_available))
        DetailRow(stringResource(R.string.expert_created_at), formatDate(optimizationCase.createdAt))
        optimizationCase.optimizationNote?.let {
            DetailRow(stringResource(R.string.expert_optimization_note), it)
        }

        actionErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        when (optimizationCase.status) {
            CaseStatus.ATANDI -> Button(
                onClick = { onChangeStatus(CaseStatus.OPTIMIZE_EDILIYOR) },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.expert_start_working)) }

            CaseStatus.OPTIMIZE_EDILIYOR -> {
                OutlinedButton(
                    onClick = { onChangeStatus(CaseStatus.TEST_EDILIYOR) },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.expert_start_test)) }
                Button(
                    onClick = onCompleteClick,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.expert_complete_optimization)) }
            }

            else -> Text(
                stringResource(R.string.expert_no_available_actions),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptimizationNoteSheet(
    isSubmitting: Boolean,
    backendError: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var submitAttempted by remember { mutableStateOf(false) }
    val normalizedNote = note.trim()
    val containsInvalidCharacters = '<' in note || '>' in note
    val isValid = normalizedNote.isNotEmpty() && note.length <= 1000 && !containsInvalidCharacters

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.expert_complete_optimization), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 1000) note = it },
                label = { Text(stringResource(R.string.expert_optimization_note)) },
                minLines = 4,
                maxLines = 7,
                isError = submitAttempted && !isValid,
                supportingText = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (submitAttempted && normalizedNote.isEmpty()) {
                            Text(stringResource(R.string.error_optimization_note_required))
                        } else if (submitAttempted && containsInvalidCharacters) {
                            Text(stringResource(R.string.error_invalid_optimization_note))
                        } else {
                            Text("")
                        }
                        Text("${note.length}/1000")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            backendError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    submitAttempted = true
                    if (isValid) onSubmit(normalizedNote)
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSubmitting) stringResource(R.string.expert_completing) else stringResource(R.string.expert_complete))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun CaseStatus.displayName(): String = name.toDisplayText()

private fun String.toDisplayText(): String = lowercase()
    .split('_')
    .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.getDefault()) } }

private fun Double?.toPercentage(): String = this
    ?.let { "${(it * 100).roundToInt()}%" }
    ?: "—"

private fun Long?.toSlaText(): String = when {
    this == null -> "—"
    this < 0 -> "SLA exceeded"
    else -> "%02d:%02d".format(this / 3600, (this % 3600) / 60)
}

private fun formatDate(value: String): String = runCatching {
    DateTimeFormatter
        .ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
        .format(Instant.parse(value).atZone(ZoneId.systemDefault()))
}.getOrDefault(value)
