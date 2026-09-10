package com.example.offerhub.screens.subscriber

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.offerhub.components.NavigationActionCard
import com.example.offerhub.components.OfferCard
import com.example.offerhub.components.SubscriberBottomBar
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.components.RefreshableContent
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.R
import com.example.offerhub.data.mock.MockOfferData
import com.example.offerhub.ui.theme.OfferHubTheme
import com.example.offerhub.viewModel.SubscriberHomeSummary
import com.example.offerhub.viewModel.buildSubscriberHomeSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberHomeScreen(
    firstName: String,
    recommendedOffers: List<Offer>,
    latestAcceptedOffer: Offer?,
    homeSummary: SubscriberHomeSummary,
    isLoading: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onRefresh: () -> Unit,
    onOfferClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onOffersClick: () -> Unit,
    onAcceptedOffersClick: () -> Unit,
    onRatedOffersClick: () -> Unit,
    onProfileClick: () -> Unit
) {

    val recommendedState = rememberLazyListState()
    val recommendedFling = rememberSnapFlingBehavior(
        lazyListState = recommendedState
    )

    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background,

        contentColor =
            MaterialTheme.colorScheme.onBackground,
        topBar = {
            OfferHubTopBar()
        },

        bottomBar = {
            SubscriberBottomBar(
                selectedItem = "home",
                onHomeClick = onHomeClick,
                onOffersClick = onOffersClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        RefreshableContent(
            isRefreshing = isLoading &&
                (recommendedOffers.isNotEmpty() || latestAcceptedOffer != null),
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),

            contentPadding = PaddingValues(
                top = 20.dp,
                bottom = 20.dp
            ),

            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            item {
                Text(
                    text = if (firstName.isBlank()) {
                        stringResource(R.string.subscriber_hello_generic)
                    } else {
                        stringResource(R.string.subscriber_hello, firstName)
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }


            if (isLoading && recommendedOffers.isEmpty() && latestAcceptedOffer == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (errorMessage != null && recommendedOffers.isEmpty() && latestAcceptedOffer == null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetryClick) {
                            Text(stringResource(R.string.profile_retry))
                        }
                    }
                }
            } else {
            // RECOMMENDED OFFERS
            item {
                Text(
                    text = stringResource(R.string.subscriber_recommended),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                if (recommendedOffers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.subscriber_no_recommended_offers),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    LazyRow(
                        state = recommendedState,
                        flingBehavior = recommendedFling,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {

                        items(recommendedOffers) { offer ->

                            OfferCard(
                                offer = offer,
                                modifier = Modifier.width(300.dp),
                                onClick = {
                                    onOfferClick(offer.offerId)
                                }
                            )
                        }
                    }
                }
            }


            // RECENTLY ACCEPTED OFFERS
            item {
                Text(
                    text = stringResource(R.string.subscriber_recently_accepted),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                if (latestAcceptedOffer != null) {
                    OfferCard(
                        offer = latestAcceptedOffer,
                        onClick = {
                            onOfferClick(latestAcceptedOffer.offerId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.subscriber_no_accepted_offer),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.subscriber_pending_ratings),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            if (homeSummary.pendingRatingOffers.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.subscriber_no_pending_ratings),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                items(
                    items = homeSummary.pendingRatingOffers,
                    key = Offer::offerId
                ) { offer ->
                    PendingRatingCard(
                        offer = offer,
                        onOfferClick = onOfferClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.subscriber_offer_journey),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                OfferJourney(
                    summary = homeSummary,
                    onAvailableClick = onOffersClick,
                    onAcceptedClick = onAcceptedOffersClick,
                    onRatedClick = onRatedOffersClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
            }
            }
        }
    }
}

@Composable
private fun PendingRatingCard(
    offer: Offer,
    onOfferClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onOfferClick(offer.offerId) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = offer.title,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.subscriber_tap_to_rate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OfferJourney(
    summary: SubscriberHomeSummary,
    onAvailableClick: () -> Unit,
    onAcceptedClick: () -> Unit,
    onRatedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        JourneyStatCard(
            label = stringResource(R.string.subscriber_journey_available),
            count = summary.availableCount,
            onClick = onAvailableClick,
            modifier = Modifier.weight(1f)
        )
        JourneyStatCard(
            label = stringResource(R.string.subscriber_journey_accepted),
            count = summary.acceptedCount,
            onClick = onAcceptedClick,
            modifier = Modifier.weight(1f)
        )
        JourneyStatCard(
            label = stringResource(R.string.subscriber_journey_rated),
            count = summary.ratedCount,
            onClick = onRatedClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun JourneyStatCard(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SubscriberHomeScreenPreview() {
    // TODO: Remove temporary subscriber previews after real backend integration is testable.
    OfferHubTheme {
        SubscriberHomeScreen(
            firstName = "Test",
            recommendedOffers = MockOfferData.offers
                .filter { it.status == OfferStatus.PENDING }
                .sortedByDescending { it.score }
                .take(3),
            latestAcceptedOffer = MockOfferData.offers
                .filter { it.status == OfferStatus.ACCEPTED }
                .maxByOrNull { it.acceptedAt.orEmpty() },
            homeSummary = buildSubscriberHomeSummary(MockOfferData.offers),
            isLoading = false,
            errorMessage = null,
            onRetryClick = {},
            onRefresh = {},
            onOfferClick = {},
            onHomeClick = {},
            onOffersClick = {},
            onAcceptedOffersClick = {},
            onRatedOffersClick = {},
            onProfileClick = {}
        )
    }
}
