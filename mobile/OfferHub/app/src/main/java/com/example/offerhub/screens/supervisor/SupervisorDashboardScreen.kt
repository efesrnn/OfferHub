package com.example.offerhub.screens.supervisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.SupervisorBottomBar
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.data.model.supervisor.SupervisorDashboard
import com.example.offerhub.data.model.supervisor.ExpertPerformanceSummary
import com.example.offerhub.data.model.supervisor.SupervisorCaseSummary
import com.example.offerhub.data.model.campaign.CaseStatus
import com.example.offerhub.data.model.campaign.Segment
import com.example.offerhub.data.model.campaign.Priority

@Composable
fun SupervisorDashboardScreen(
    dashboard: SupervisorDashboard?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onActiveCasesClick: () -> Unit,
    onPendingAssignmentClick: () -> Unit,
    onExpertsClick: () -> Unit,
    onCasesClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        topBar = { OfferHubTopBar() },
        bottomBar = {
            SupervisorBottomBar("home", {}, onCasesClick, onProfileClick)
        }
    ) { padding ->
        when {
            isLoading && dashboard == null -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            errorMessage != null && dashboard == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetryClick, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.admin_try_again))
                }
            }
            dashboard != null -> DashboardContent(
                dashboard = dashboard,
                onActiveCasesClick = onActiveCasesClick,
                onPendingAssignmentClick = onPendingAssignmentClick,
                onExpertsClick = onExpertsClick,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DashboardContent(
    dashboard: SupervisorDashboard,
    onActiveCasesClick: () -> Unit,
    onPendingAssignmentClick: () -> Unit,
    onExpertsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val attentionCases = dashboard.attentionCases
        .filter { it.status in activeCaseStatuses && (it.slaRemainingSeconds ?: Long.MAX_VALUE) <= 0L }
        .sortedWith(
            compareBy<SupervisorCaseSummary> { it.slaRemainingSeconds ?: Long.MAX_VALUE }
                .thenByDescending { it.priority.sortWeight }
        )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.supervisor_dashboard), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(stringResource(R.string.supervisor_ai_accuracy), "${dashboard.aiAccuracyPercent}%", Modifier.weight(1f))
                MetricCard(stringResource(R.string.supervisor_sla_compliance), "${dashboard.slaCompliancePercent}%", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(stringResource(R.string.supervisor_active_cases), dashboard.activeCaseCount.toString(), Modifier.weight(1f), onActiveCasesClick)
                MetricCard(stringResource(R.string.supervisor_pending_assignment), dashboard.pendingAssignmentCount.toString(), Modifier.weight(1f), onPendingAssignmentClick)
            }
        }
        item { SectionTitle(stringResource(R.string.supervisor_segment_distribution)) }
        item {
            SegmentBarChart(dashboard)
        }
        item { SectionTitle(stringResource(R.string.supervisor_conversion_trend)) }
        item {
            ConversionLineChart(dashboard)
        }
        item { SectionTitle(stringResource(R.string.supervisor_attention_cases)) }
        if (attentionCases.isEmpty()) {
            item { EmptyMessage(stringResource(R.string.supervisor_no_attention_cases)) }
        }
        items(attentionCases, key = { it.caseId }) { item ->
            val hasBreachedSla = item.slaRemainingSeconds != null && item.slaRemainingSeconds < 0
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (hasBreachedSla) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Text(item.priority.displayName(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.assignedExpertId ?: stringResource(R.string.supervisor_waiting_assignment))
                    Text(
                        item.slaRemainingSeconds.dashboardSlaText(),
                        color = if (hasBreachedSla) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (hasBreachedSla) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        item { SectionTitle(stringResource(R.string.supervisor_expert_performance)) }
        if (dashboard.expertPerformance.isEmpty()) {
            item { EmptyMessage(stringResource(R.string.supervisor_no_experts)) }
        } else {
            items(dashboard.expertPerformance.take(3), key = { it.expertId }) { expert ->
                ExpertSummaryCard(expert)
            }
            item {
                Button(onClick = onExpertsClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.supervisor_view_experts))
                }
            }
        }
    }
}

private val activeCaseStatuses = setOf(
    CaseStatus.ATANDI,
    CaseStatus.OPTIMIZE_EDILIYOR,
    CaseStatus.TEST_EDILIYOR
)

private val Priority.sortWeight: Int
    get() = when (this) {
        Priority.KRITIK -> 4
        Priority.YUKSEK -> 3
        Priority.ORTA -> 2
        Priority.DUSUK -> 1
        Priority.UNKNOWN -> 0
    }

@Composable
private fun Long?.dashboardSlaText(): String = when {
    this == null -> stringResource(R.string.supervisor_sla_unknown)
    this < 0 -> stringResource(R.string.supervisor_sla_exceeded)
    else -> stringResource(
        R.string.supervisor_sla_time_value,
        this / 3600,
        (this % 3600) / 60
    )
}

@Composable
private fun ExpertSummaryCard(expert: ExpertPerformanceSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(expert.displayName, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.supervisor_expert_summary, expert.completedCases, expert.averageConversionIncrease, expert.averageCompletionHours),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (onClick == null) {
        Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            MetricCardContent(label, value)
        }
    } else {
        Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            MetricCardContent(label, value)
        }
    }
}

@Composable
private fun MetricCardContent(label: String, value: String) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

@Composable
private fun SegmentBarChart(dashboard: SupervisorDashboard) {
    val maxCount = dashboard.segmentDistribution.maxOfOrNull { it.campaignCount }?.coerceAtLeast(1) ?: 1
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            dashboard.segmentDistribution.forEach { item ->
                Text(item.segment.displayName(), style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).height(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(item.campaignCount.toFloat() / maxCount).height(10.dp)
                                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        )
                    }
                    Text(item.campaignCount.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

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
private fun ConversionLineChart(dashboard: SupervisorDashboard) {
    val points = dashboard.conversionTrend
    val lineColor = MaterialTheme.colorScheme.primary
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                if (points.size > 1) {
                    val min = points.minOf { it.conversionPercent }
                    val max = points.maxOf { it.conversionPercent }
                    val range = (max - min).takeIf { it > 0 } ?: 1.0
                    val coordinates = points.mapIndexed { index, point ->
                        androidx.compose.ui.geometry.Offset(
                            x = size.width * index / (points.size - 1),
                            y = size.height - ((point.conversionPercent - min) / range * size.height).toFloat()
                        )
                    }
                    coordinates.zipWithNext().forEach { (start, end) -> drawLine(lineColor, start, end, strokeWidth = 6f) }
                    coordinates.forEach { drawCircle(lineColor, radius = 8f, center = it) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                points.forEach { Text("${it.period}\n${it.conversionPercent}%", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

