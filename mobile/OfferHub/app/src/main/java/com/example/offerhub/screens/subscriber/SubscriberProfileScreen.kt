package com.example.offerhub.screens.subscriber

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.components.SubscriberBottomBar
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.R
import com.example.offerhub.ui.theme.OfferHubTheme

@Composable
fun SubscriberProfileScreen(
    firstName: String,
    lastName: String,
    phone: String,
    email: String?,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOffersClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor =
            MaterialTheme.colorScheme.onBackground,

        topBar = {
            OfferHubTopBar()
        },
        bottomBar = {
            SubscriberBottomBar(
                selectedItem = "profile",
                onHomeClick = onHomeClick,
                onOffersClick = onOffersClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(onClick = onRetryClick) {
                        Text(text = stringResource(R.string.profile_retry))
                    }
                }

                else -> {
                    ProfileInfoCard(
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        email = email
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.profile_log_out))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    firstName: String,
    lastName: String,
    phone: String,
    email: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfileInfoRow(
                label = stringResource(R.string.profile_name),
                value = "$firstName $lastName"
            )

            ProfileInfoRow(
                label = stringResource(R.string.profile_phone),
                value = phone
            )

            ProfileInfoRow(
                label = stringResource(R.string.profile_email),
                value = email?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.profile_not_provided)
            )

        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize=14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscriberProfileScreenPreview() {
    // TODO: Remove temporary subscriber previews after real backend integration is testable.
    OfferHubTheme {
        SubscriberProfileScreen(
            firstName = "Test",
            lastName = "Subscriber",
            phone = "+90 555 111 22 33",
            email = "test@offerhub.com",
            onRetryClick = {},
            onLogoutClick = {},
            onHomeClick = {},
            onOffersClick = {},
            onProfileClick = {}
        )
    }
}
