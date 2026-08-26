package com.example.offerhub.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.components.AdminBottomBar
import com.example.offerhub.R

@Composable
fun AdminHomeScreen(
    onCreateStaffClick: () -> Unit,
    onSearchStaffClick: () -> Unit,
    onUpdateRoleClick: () -> Unit,
    onAuditLogsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        topBar = { OfferHubTopBar() },
        bottomBar = {
            AdminBottomBar(
                selectedItem = "home",
                onHomeClick = {},
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_dashboard),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            AdminActionCard(
                title = stringResource(R.string.admin_search_staff),
                description = stringResource(R.string.admin_search_staff_description),
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                onClick = onSearchStaffClick
            )
            AdminActionCard(
                title = stringResource(R.string.admin_create_staff),
                description = stringResource(R.string.admin_create_staff_description),
                icon = { Icon(Icons.Default.ManageAccounts, contentDescription = null) },
                onClick = onCreateStaffClick
            )
            AdminActionCard(
                title = stringResource(R.string.admin_update_role),
                description = stringResource(R.string.admin_update_role_description),
                icon = { Icon(Icons.Default.ManageAccounts, contentDescription = null) },
                onClick = onUpdateRoleClick
            )
            AdminActionCard(
                title = stringResource(R.string.admin_audit_logs),
                description = stringResource(R.string.admin_audit_logs_description),
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                onClick = onAuditLogsClick
            )
        }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

