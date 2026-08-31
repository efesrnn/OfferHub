package com.example.offerhub.screens.supervisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.SupervisorBottomBar
import com.example.offerhub.components.OfferHubTopBar

@Composable
fun SupervisorProfileScreen(
    userId: String,
    onHomeClick: () -> Unit,
    onCasesClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        topBar = { OfferHubTopBar() },
        bottomBar = { SupervisorBottomBar("profile", onHomeClick, onCasesClick, {}) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.supervisor_profile), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.admin_user_id), style = MaterialTheme.typography.labelMedium)
                    Text(userId)
                    Text(stringResource(R.string.admin_role), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.role_supervisor))
                }
            }
            OutlinedButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_log_out))
            }
        }
    }
}
