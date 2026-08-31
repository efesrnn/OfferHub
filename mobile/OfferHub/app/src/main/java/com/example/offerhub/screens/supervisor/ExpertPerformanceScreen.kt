package com.example.offerhub.screens.supervisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.data.model.supervisor.ExpertPerformanceSummary

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SupervisorExpertPerformanceScreen(
    experts: List<ExpertPerformanceSummary>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedExpert by remember { mutableStateOf<ExpertPerformanceSummary?>(null) }
    val filteredExperts = remember(experts, query) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) experts else experts.filter {
            it.displayName.contains(normalizedQuery, ignoreCase = true)
        }
    }
    Scaffold(topBar = { OfferHubDetailTopBar(stringResource(R.string.supervisor_experts), onBackClick) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading && experts.isEmpty()) {
                item {
                    Column(
                        Modifier.fillParentMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) { CircularProgressIndicator() }
                }
            } else if (errorMessage != null && experts.isEmpty()) {
                item {
                    Column(
                        Modifier.fillParentMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetryClick) { Text(stringResource(R.string.admin_try_again)) }
                    }
                }
            } else item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.supervisor_search_expert)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!isLoading && errorMessage == null && filteredExperts.isEmpty()) {
                item { Text(stringResource(R.string.supervisor_no_experts), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(filteredExperts, key = { it.expertId }) { expert ->
                Card(
                    onClick = { selectedExpert = expert },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(expert.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    selectedExpert?.let { expert ->
        ModalBottomSheet(onDismissRequest = { selectedExpert = null }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(expert.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                ExpertDetailRow(stringResource(R.string.supervisor_expert_id), expert.expertId)
                ExpertDetailRow(stringResource(R.string.supervisor_completed_cases), expert.completedCases.toString())
                ExpertDetailRow(stringResource(R.string.supervisor_average_conversion_increase), "${expert.averageConversionIncrease}%")
                ExpertDetailRow(stringResource(R.string.supervisor_average_completion_time), "${expert.averageCompletionHours} h")
                OutlinedButton(
                    onClick = { selectedExpert = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.admin_close)) }
            }
        }
    }
}

@Composable
private fun ExpertDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
