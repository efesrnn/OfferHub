package com.example.offerhub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import java.util.Locale
import com.example.offerhub.R
import com.example.offerhub.data.model.campaign.OptimizationCase
import com.example.offerhub.data.model.campaign.Priority

@Composable
fun ExpertBottomBar(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onOperationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    OfferHubBottomBar(
        selectedItem = selectedItem,
        items = listOf(
            BottomBarItem("home", Icons.Default.Home, R.string.expert_home, onHomeClick),
            BottomBarItem("operations", Icons.Default.Work, R.string.expert_operations, onOperationsClick),
            BottomBarItem("profile", Icons.Default.Person, R.string.nav_profile, onProfileClick)
        )
    )
}

@Composable
fun ExpertCaseCard(
    optimizationCase: OptimizationCase,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    optimizationCase.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    optimizationCase.priority.name.toDisplayText(),
                    color = priorityColor(optimizationCase.priority),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(optimizationCase.campaignNo, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(optimizationCase.segment.name.toDisplayText(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${stringResource(R.string.expert_status)}: ${optimizationCase.status.name.toDisplayText()}")
            Text(
                text = slaText(optimizationCase.slaRemainingSeconds),
                color = slaColor(optimizationCase.priority, optimizationCase.slaRemainingSeconds)
            )
        }
    }
}

@Composable
private fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.KRITIK -> MaterialTheme.colorScheme.error
    Priority.YUKSEK -> if (isSystemInDarkTheme()) Color(0xFFFFB86C) else Color(0xFFB45309)
    Priority.ORTA -> MaterialTheme.colorScheme.primary
    Priority.DUSUK -> MaterialTheme.colorScheme.onSurfaceVariant
    Priority.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun String.toDisplayText(): String = lowercase()
    .split('_')
    .joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

@Composable
private fun slaColor(priority: Priority, remainingSeconds: Long?): Color = when {
    remainingSeconds == null -> MaterialTheme.colorScheme.onSurfaceVariant
    remainingSeconds < 0 -> MaterialTheme.colorScheme.error
    priority == Priority.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    remainingSeconds <= slaTotalSeconds(priority) / 4 ->
        if (isSystemInDarkTheme()) Color(0xFFFFB86C) else Color(0xFFB45309)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun slaTotalSeconds(priority: Priority): Long = when (priority) {
    Priority.KRITIK -> 2 * 60 * 60L
    Priority.YUKSEK -> 8 * 60 * 60L
    Priority.ORTA -> 24 * 60 * 60L
    Priority.DUSUK -> 72 * 60 * 60L
    Priority.UNKNOWN -> Long.MAX_VALUE
}

@Composable
private fun slaText(remainingSeconds: Long?): String = when {
    remainingSeconds == null -> stringResource(R.string.expert_sla_not_available)
    remainingSeconds < 0 -> stringResource(R.string.expert_sla_exceeded)
    else -> stringResource(R.string.expert_sla_remaining, formatRemainingTime(remainingSeconds))
}

private fun formatRemainingTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return "%02d:%02d".format(hours, minutes)
}
