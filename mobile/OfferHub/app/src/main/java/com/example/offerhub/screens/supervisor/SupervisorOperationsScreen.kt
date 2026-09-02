package com.example.offerhub.screens.supervisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.components.SupervisorBottomBar

@Composable
fun SupervisorOperationsScreen(
    onPendingAssignmentClick: () -> Unit,
    onActiveCasesClick: () -> Unit,
    onApprovalQueueClick: () -> Unit,
    onPublishedCasesClick: () -> Unit,
    onExpertPerformanceClick: () -> Unit,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        topBar = { OfferHubTopBar() },
        bottomBar = { SupervisorBottomBar("operations", onHomeClick, {}, onProfileClick) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text(stringResource(R.string.supervisor_operations), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { OperationCard(stringResource(R.string.supervisor_pending_assignment), stringResource(R.string.supervisor_pending_assignment_description), onPendingAssignmentClick) }
            item { OperationCard(stringResource(R.string.supervisor_active_cases), stringResource(R.string.supervisor_active_cases_description), onActiveCasesClick) }
            item { OperationCard(stringResource(R.string.supervisor_approval_queue), stringResource(R.string.supervisor_approval_queue_description), onApprovalQueueClick) }
            item { OperationCard(stringResource(R.string.supervisor_published_cases), stringResource(R.string.supervisor_published_cases_description), onPublishedCasesClick) }
            item { OperationCard(stringResource(R.string.supervisor_experts), stringResource(R.string.supervisor_experts_description), onExpertPerformanceClick) }
        }
    }
}

@Composable
private fun OperationCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
