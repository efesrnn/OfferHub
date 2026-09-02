package com.example.offerhub.screens.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.ExpertBottomBar
import com.example.offerhub.components.ExpertCaseCard
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.data.model.campaign.OptimizationCase
import com.example.offerhub.data.model.campaign.Priority
import com.example.offerhub.data.model.campaign.CaseStatus

@Composable
fun ExpertHomeScreen(
    cases: List<OptimizationCase>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onCaseClick: (String) -> Unit,
    onOperationsClick: () -> Unit,
    onCriticalCasesClick: () -> Unit,
    onActiveCasesClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val activeCases = cases.filter { it.status in expertActiveStatuses }
    Scaffold(
        topBar = { OfferHubTopBar() },
        bottomBar = {
            ExpertBottomBar(
                selectedItem = "home",
                onHomeClick = {},
                onOperationsClick = onOperationsClick,
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->
        when {
            isLoading -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            errorMessage != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetryClick, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.admin_try_again))
                }
            }
            cases.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.expert_empty_cases),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.expert_empty_cases_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.expert_dashboard),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpertMetricCard(
                            title = stringResource(R.string.expert_critical_cases),
                            value = activeCases.count { it.priority == Priority.KRITIK },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            onClick = onCriticalCasesClick,
                            modifier = Modifier.weight(1f)
                        )
                        ExpertMetricCard(
                            title = stringResource(R.string.expert_active_cases),
                            value = activeCases.size,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onActiveCasesClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item { Text(stringResource(R.string.expert_priority_cases), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(activeCases.take(3), key = { it.caseId }) { case ->
                    ExpertCaseCard(case, onClick = { onCaseClick(case.caseId) })
                }
            }
        }
    }
}

private val expertActiveStatuses = setOf(
    CaseStatus.ATANDI,
    CaseStatus.OPTIMIZE_EDILIYOR,
    CaseStatus.TEST_EDILIYOR
)

@Composable
private fun ExpertMetricCard(
    title: String,
    value: Int,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(120.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = contentColor)
            Text(title, color = contentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}
