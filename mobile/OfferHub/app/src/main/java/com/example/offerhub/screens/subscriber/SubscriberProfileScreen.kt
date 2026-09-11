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
import com.example.offerhub.data.model.SubscriberInsight
import com.example.offerhub.ui.text.adminCodeLabel
import com.example.offerhub.ui.theme.OfferHubTheme

@Composable
fun SubscriberProfileScreen(
    firstName: String,
    lastName: String,
    phone: String,
    email: String?,
    insight: SubscriberInsight? = null,
    isInsightLoading: Boolean = false,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    AiInsightCard(
                        insight = insight,
                        isLoading = isInsightLoading
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
    val fullName = listOf(firstName, lastName)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString(" ")
        .ifBlank { stringResource(R.string.profile_not_available) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfileInfoRow(
                label = stringResource(R.string.profile_name),
                value = fullName
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

/**
 * Abonenin AI segmentini VE onu ayirt eden ham sinyalleri gosterir - amac, "PASIF" gibi
 * tek bir etiketin arkasindaki gercek veriyi gorunur kilmak (iki PASIF abone farkli
 * sebeplerle PASIF olabilir). Bilgilendirici/ek bir alan oldugu icin yuklenemezse sessizce
 * hic gorunmez, ana profil akisini bir hata mesajiyla bozmaz.
 */
@Composable
private fun AiInsightCard(
    insight: SubscriberInsight?,
    isLoading: Boolean
) {
    if (isLoading) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            }
        }
        return
    }
    if (insight == null) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_ai_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProfileInfoRow(
                label = stringResource(R.string.profile_ai_segment_label),
                value = adminCodeLabel(insight.segment)
            )

            ProfileInfoRow(
                label = stringResource(R.string.profile_ai_reason_label),
                value = insight.reason
            )

            ProfileInfoRow(
                label = stringResource(R.string.profile_ai_usage_label),
                value = "%.1f GB/ay".format(insight.monthlyDataUsageGb)
            )

            ProfileInfoRow(
                label = stringResource(R.string.profile_ai_spend_label),
                value = "%.0f TL/ay".format(insight.monthlySpendTry)
            )

            ProfileInfoRow(
                label = stringResource(R.string.profile_ai_complaints_label),
                value = insight.complaintCount6m.toString()
            )

            ProfileInfoRow(
                label = stringResource(R.string.profile_ai_trend_label),
                value = if (insight.usageTrend >= 0) "+%.2f".format(insight.usageTrend) else "%.2f".format(insight.usageTrend)
            )
        }
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
            insight = SubscriberInsight(
                segment = "PASIF",
                reason = "Veri kullanimi cok dusuk (3.2 GB/ay)",
                tenureMonths = 4,
                monthlyDataUsageGb = 3.2,
                monthlySpendTry = 120.0,
                complaintCount6m = 0,
                usageTrend = -0.05,
                pastAcceptedOffers = 0,
                pastDeclinedOffers = 0,
                currentTariff = "EKONOMIK"
            ),
            onRetryClick = {},
            onLogoutClick = {},
            onHomeClick = {},
            onOffersClick = {},
            onProfileClick = {}
        )
    }
}
