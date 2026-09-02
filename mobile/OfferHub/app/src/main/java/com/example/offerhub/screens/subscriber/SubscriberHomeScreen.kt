package com.example.offerhub.screens.subscriber

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.example.offerhub.components.NavigationActionCard
import com.example.offerhub.components.OfferCard
import com.example.offerhub.components.SubscriberBottomBar
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.model.OfferType
import com.example.offerhub.R
import com.example.offerhub.data.mock.MockOfferData
import com.example.offerhub.ui.theme.OfferHubTheme

@Composable
fun SubscriberHomeScreen(
    firstName: String,
    recommendedOffers: List<Offer>,
    latestAcceptedOffer: Offer?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetryClick: () -> Unit,
    onOfferClick: (String) -> Unit,
    onCategoryClick: (OfferType) -> Unit,
    onHomeClick: () -> Unit,
    onOffersClick: () -> Unit,
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),

            contentPadding = PaddingValues(
                top = 20.dp,
                bottom = 20.dp
            ),

            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            item {
                Text(
                    text = stringResource(R.string.subscriber_hello, firstName),
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
                        isAccepted = true,
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
                    text = stringResource(R.string.subscriber_quick_actions),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding =
                        PaddingValues(horizontal = 24.dp),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuickActionCard(
                            title = stringResource(R.string.offers_add_on_packages),
                            onClick = {
                                onCategoryClick(OfferType.ADD_ON)
                            }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = stringResource(R.string.offers_tariff_upgrade),
                            onClick = {
                                onCategoryClick(
                                    OfferType.TARIFF_UPGRADE
                                )
                            }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = stringResource(R.string.offers_device),
                            onClick = {
                                onCategoryClick(
                                    OfferType.DEVICE_OFFER
                                )
                            }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = stringResource(R.string.offers_loyalty),
                            onClick = {
                                onCategoryClick(OfferType.LOYALTY)
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(96.dp)
            .clickable(onClick = onClick),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh,

            contentColor =
                MaterialTheme.colorScheme.onSurface
        ),

        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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
            recommendedOffers = MockOfferData.offers.filter { it.status == OfferStatus.PENDING },
            latestAcceptedOffer = MockOfferData.offers
                .filter { it.status == OfferStatus.ACCEPTED }
                .maxByOrNull { it.acceptedAt.orEmpty() },
            isLoading = false,
            errorMessage = null,
            onRetryClick = {},
            onOfferClick = {},
            onCategoryClick = {},
            onHomeClick = {},
            onOffersClick = {},
            onProfileClick = {}
        )
    }
}
