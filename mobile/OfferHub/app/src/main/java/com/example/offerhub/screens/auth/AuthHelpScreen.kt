package com.example.offerhub.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.ui.theme.OfferHubTheme

@Composable
fun AuthHelpScreen(
    onBackClick: () -> Unit,
    onSubscriberLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Scaffold(
        topBar = {
            OfferHubDetailTopBar(
                title = stringResource(R.string.auth_help_title),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.auth_help_intro),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                HelpCard(
                    title = stringResource(R.string.auth_help_otp_title),
                    description = stringResource(R.string.auth_help_otp_description),
                    onClick = onSubscriberLoginClick
                )
            }
            item {
                HelpCard(
                    title = stringResource(R.string.auth_help_account_title),
                    description = stringResource(R.string.auth_help_account_description),
                    onClick = null
                )
            }
            item {
                HelpCard(
                    title = stringResource(R.string.auth_help_staff_title),
                    description = stringResource(R.string.auth_help_staff_description),
                    onClick = null
                )
            }
            item {
                HelpCard(
                    title = stringResource(R.string.auth_help_forgot_password_title),
                    description = stringResource(R.string.auth_help_forgot_password_description),
                    onClick = onForgotPasswordClick
                )
            }
        }
    }
}

@Composable
private fun HelpCard(
    title: String,
    description: String,
    onClick: (() -> Unit)?
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
    if (onClick == null) {
        Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
            HelpCardContent(title, description, showNavigationIcon = false)
        }
    } else {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = colors) {
            HelpCardContent(title, description, showNavigationIcon = true)
        }
    }
}

@Composable
private fun HelpCardContent(
    title: String,
    description: String,
    showNavigationIcon: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showNavigationIcon) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthHelpScreenPreview() {
    OfferHubTheme {
        AuthHelpScreen(
            onBackClick = {},
            onSubscriberLoginClick = {},
            onForgotPasswordClick = {}
        )
    }
}
