package com.example.offerhub.screens.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offerhub.R
import com.example.offerhub.components.OfferHubDetailTopBar
import com.example.offerhub.data.model.campaign.Campaign
import com.example.offerhub.data.model.campaign.CampaignStatus
import com.example.offerhub.data.model.campaign.Segment
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertCampaignListScreen(
    campaigns: List<Campaign>,
    isLoading: Boolean,
    isLoadingNextPage: Boolean,
    canLoadMore: Boolean,
    errorMessage: String?,
    selectedStatus: CampaignStatus?,
    selectedSegment: Segment?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCreateClick: () -> Unit,
    onLoadNextPage: () -> Unit,
    onApplyFilters: (CampaignStatus?, Segment?) -> Unit,
    onCampaignClick: (String) -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val filterCount = (if (selectedStatus == null) 0 else 1) + (if (selectedSegment == null) 0 else 1)
    val shouldLoadNextPage by remember(listState, canLoadMore, isLoadingNextPage) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            canLoadMore && !isLoadingNextPage && lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadNextPage, campaigns.size) {
        if (shouldLoadNextPage) onLoadNextPage()
    }
    Scaffold(topBar = { OfferHubDetailTopBar(stringResource(R.string.expert_campaigns), onBackClick) }) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            errorMessage != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetryClick, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.admin_try_again))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = { showFilters = true }, modifier = Modifier.weight(1f)) {
                            Text(
                                if (filterCount == 0) stringResource(R.string.expert_filters)
                                else stringResource(R.string.expert_filters_count, filterCount)
                            )
                        }
                        Button(onClick = onCreateClick, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.expert_create_campaign))
                        }
                    }
                }
                if (campaigns.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.expert_no_campaigns),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(campaigns, key = { it.campaignNo }) { campaign ->
                        Card(
                            onClick = { onCampaignClick(campaign.campaignNo) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(campaign.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(campaign.campaignNo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(campaign.type.name.displayText())
                                Text(campaign.status.name.displayText(), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (isLoadingNextPage) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        CampaignFilterSheet(
            currentStatus = selectedStatus,
            currentSegment = selectedSegment,
            onDismiss = { showFilters = false },
            onApply = { status, segment ->
                onApplyFilters(status, segment)
                showFilters = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampaignFilterSheet(
    currentStatus: CampaignStatus?,
    currentSegment: Segment?,
    onDismiss: () -> Unit,
    onApply: (CampaignStatus?, Segment?) -> Unit
) {
    var draftStatus by remember(currentStatus) { mutableStateOf(currentStatus) }
    var draftSegment by remember(currentSegment) { mutableStateOf(currentSegment) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.expert_filter_campaigns), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.expert_status_filter), fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = draftStatus == null, onClick = { draftStatus = null }, label = { Text(stringResource(R.string.expert_filter_all)) })
                }
                items(CampaignStatus.entries.filterNot { it == CampaignStatus.UNKNOWN }) { status ->
                    FilterChip(selected = draftStatus == status, onClick = { draftStatus = status }, label = { Text(status.name.displayText()) })
                }
            }
            Text(stringResource(R.string.expert_target_segment), fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = draftSegment == null, onClick = { draftSegment = null }, label = { Text(stringResource(R.string.expert_filter_all)) })
                }
                items(Segment.entries.filterNot { it == Segment.UNKNOWN }) { segment ->
                    FilterChip(selected = draftSegment == segment, onClick = { draftSegment = segment }, label = { Text(segment.name.displayText()) })
                }
            }
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { draftStatus = null; draftSegment = null },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.admin_clear)) }
                Button(
                    onClick = { onApply(draftStatus, draftSegment) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.admin_apply)) }
            }
        }
    }
}

private fun String.displayText(): String = lowercase().split('_').joinToString(" ") {
    it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
}
