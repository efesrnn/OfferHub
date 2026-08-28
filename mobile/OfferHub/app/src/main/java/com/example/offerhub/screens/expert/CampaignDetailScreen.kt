package com.example.offerhub.screens.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.data.model.campaign.Campaign
import java.util.Locale

@Composable
fun ExpertCampaignDetailScreen(
    campaign: Campaign?,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Scaffold(topBar = { OfferHubDetailTopBar(stringResource(R.string.expert_campaign_detail), onBackClick) }) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            errorMessage != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetryClick, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.admin_try_again)) }
            }
            campaign != null -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(campaign.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CampaignValue(stringResource(R.string.expert_campaign_number), campaign.campaignNo)
                        CampaignValue(stringResource(R.string.expert_campaign_type), campaign.type.name.readable())
                        CampaignValue(stringResource(R.string.expert_target_segment), campaign.targetSegment.name.readable())
                        CampaignValue(stringResource(R.string.expert_current_segment), campaign.segment.name.readable())
                        CampaignValue(stringResource(R.string.expert_ai_segment), campaign.aiSegment.name.readable())
                        CampaignValue(stringResource(R.string.expert_discount_rate), "${campaign.discountRate}%")
                        CampaignValue(stringResource(R.string.expert_valid_until), campaign.validUntil)
                        CampaignValue(stringResource(R.string.expert_status), campaign.status.name.readable())
                        CampaignValue(stringResource(R.string.expert_priority), campaign.priority.name.readable())
                        CampaignValue(
                            stringResource(R.string.expert_conversion_probability),
                            campaign.conversionProbability?.let { "${(it * 100).toInt()}%" } ?: stringResource(R.string.common_not_available)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampaignValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun String.readable(): String = lowercase().split('_').joinToString(" ") {
    it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
}
