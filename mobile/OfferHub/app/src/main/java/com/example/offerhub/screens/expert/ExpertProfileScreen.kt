package com.example.offerhub.screens.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.example.offerhub.components.ExpertBottomBar
import com.example.offerhub.components.OfferHubTopBar

@Composable
fun ExpertProfileScreen(
    userId: String,
    role: String,
    specialties: List<String>,
    regions: List<String>,
    onLogoutClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOperationsClick: () -> Unit
) {
    Scaffold(
        topBar = { OfferHubTopBar() },
        bottomBar = {
            ExpertBottomBar(
                selectedItem = "profile",
                onHomeClick = onHomeClick,
                onOperationsClick = onOperationsClick,
                onProfileClick = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                stringResource(R.string.expert_profile),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExpertProfileValue(stringResource(R.string.admin_user_id), userId)
                    ExpertProfileValue(stringResource(R.string.admin_role), role)
                    ExpertProfileValue(
                        stringResource(R.string.admin_specialties),
                        specialties.ifEmpty { listOf(stringResource(R.string.common_not_available)) }.joinToString()
                    )
                    ExpertProfileValue(
                        stringResource(R.string.admin_regions),
                        regions.ifEmpty { listOf(stringResource(R.string.common_not_available)) }.joinToString()
                    )
                }
            }
            Button(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_log_out))
            }
        }
    }
}

@Composable
private fun ExpertProfileValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
