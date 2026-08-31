package com.example.offerhub.screens.expert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import com.example.offerhub.data.model.gamification.ExpertLevel
import com.example.offerhub.data.model.gamification.GamificationProfile
import com.example.offerhub.data.model.gamification.RankingEntry
import com.example.offerhub.data.model.gamification.RankingPeriod
import java.text.NumberFormat

@Composable
fun ExpertProgressScreen(
    profile: GamificationProfile?,
    ranking: List<RankingEntry>,
    selectedPeriod: RankingPeriod,
    isLoading: Boolean,
    isLoadingRanking: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onPeriodSelected: (RankingPeriod) -> Unit
) {
    Scaffold(
        topBar = {
            OfferHubDetailTopBar(
                title = stringResource(R.string.expert_my_progress),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        when {
            isLoading && profile == null -> ProgressLoading(Modifier.padding(padding))
            profile == null -> ProgressError(
                message = errorMessage ?: stringResource(R.string.error_gamification),
                onRetryClick = onRetryClick,
                modifier = Modifier.padding(padding)
            )
            else -> ProgressContent(
                profile = profile,
                ranking = ranking,
                selectedPeriod = selectedPeriod,
                isLoadingRanking = isLoadingRanking,
                errorMessage = errorMessage,
                onRetryClick = onRetryClick,
                onPeriodSelected = onPeriodSelected,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ProgressContent(
    profile: GamificationProfile,
    ranking: List<RankingEntry>,
    selectedPeriod: RankingPeriod,
    isLoadingRanking: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onPeriodSelected: (RankingPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { LevelCard(profile) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(stringResource(R.string.expert_cases_short), profile.completedCases.toString(), Modifier.weight(1f))
                MetricCard(stringResource(R.string.expert_rating_short), "%.1f".format(profile.averageRating), Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(stringResource(R.string.expert_daily_short), profile.dailyRank.asRank(), Modifier.weight(1f))
                MetricCard(stringResource(R.string.expert_weekly_short), profile.weeklyRank.asRank(), Modifier.weight(1f))
            }
        }
        item {
            Text(
                text = stringResource(R.string.expert_badges),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            if (profile.badges.isEmpty()) {
                Text(
                    text = stringResource(R.string.expert_no_badges),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profile.badges, key = { it.id }) { badge ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = badge.title,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.expert_ranking),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RankingPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodSelected(period) },
                        label = {
                            Text(
                                stringResource(
                                    if (period == RankingPeriod.DAILY) R.string.expert_daily_short
                                    else R.string.expert_weekly_short
                                )
                            )
                        }
                    )
                }
            }
        }
        when {
            isLoadingRanking -> item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
            errorMessage != null -> item {
                ProgressError(errorMessage, onRetryClick, Modifier.fillMaxWidth())
            }
            ranking.isEmpty() -> item {
                Text(
                    text = stringResource(R.string.expert_no_ranking),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> items(ranking, key = { "${selectedPeriod.name}-${it.expertId}" }) { entry ->
                RankingCard(
                    entry = entry,
                    isCurrentUser = entry.expertId == profile.expertId
                )
            }
        }
    }
}

@Composable
private fun LevelCard(profile: GamificationProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = profile.level.displayName(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.expert_points_value,
                    NumberFormat.getIntegerInstance().format(profile.totalPoints)
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RankingCard(entry: RankingEntry, isCurrentUser: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("#${entry.rank}", fontWeight = FontWeight.Bold)
            Text(entry.displayName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.expert_points_value, entry.points))
        }
    }
}

@Composable
private fun ProgressLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { CircularProgressIndicator() }
}

@Composable
private fun ProgressError(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetryClick) { Text(stringResource(R.string.admin_try_again)) }
    }
}

@Composable
private fun ExpertLevel.displayName(): String = stringResource(
    when (this) {
        ExpertLevel.BRONZE -> R.string.expert_level_bronze
        ExpertLevel.SILVER -> R.string.expert_level_silver
        ExpertLevel.GOLD -> R.string.expert_level_gold
        ExpertLevel.PLATINUM -> R.string.expert_level_platinum
    }
)

private fun Int?.asRank(): String = this?.let { "#$it" } ?: "—"
