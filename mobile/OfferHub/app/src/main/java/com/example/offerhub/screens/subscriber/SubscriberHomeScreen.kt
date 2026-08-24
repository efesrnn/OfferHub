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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.components.NavigationActionCard
import com.example.offerhub.components.OfferCard
import com.example.offerhub.components.OfferHubBottomBar
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferType

@Composable
fun SubscriberHomeScreen(
    firstName: String,
    recommendedOffers: List<Offer>,
    latestAcceptedOffer: Offer?,
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
            OfferHubBottomBar(
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
                    text = "Hello, $firstName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }


            // RECOMMENDED OFFERS
            item {
                Text(
                    text = "Recommended for you",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                LazyRow(
                    state = recommendedState,
                    flingBehavior = recommendedFling,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    items(recommendedOffers) { offer ->

                        OfferCard(
                            offer = offer,
                            onClick = {
                                onOfferClick(offer.offerId)
                            }
                        )
                    }
                }
            }


            // RECENTLY ACCEPTED OFFERS
            item {
                Text(
                    text = "Recently accepted",
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
                        text = "You have not accepted an offer yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Quick actions",
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
                            title = "Add-on Packages",
                            onClick = {
                                onCategoryClick(OfferType.ADD_ON)
                            }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Tariff Upgrade",
                            onClick = {
                                onCategoryClick(
                                    OfferType.TARIFF_UPGRADE
                                )
                            }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Device Offers",
                            onClick = {
                                onCategoryClick(
                                    OfferType.DEVICE_OFFER
                                )
                            }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Loyalty Offers",
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
    val offers = listOf(
        Offer(
            offerId = "f1a2",
            campaignNo = "CMP-2026-000123",
            title = "Summer Extra Package",
            score = 0.83,
            highlighted = true,
            status = "PENDING",
            type = OfferType.ADD_ON
        ),

        Offer(
            offerId = "f1a3",
            campaignNo = "CMP-2026-000124",
            title = "Advantage Tariff",
            score = 0.76,
            highlighted = true,
            status = "PENDING",
            type = OfferType.TARIFF_UPGRADE
        ),

        Offer(
            offerId = "f1a4",
            campaignNo = "CMP-2026-000125",
            title = "Device Discount",
            score = 0.68,
            highlighted = false,
            status = "PENDING",
            type = OfferType.DEVICE_OFFER
        )
    )

    val latestAcceptedOffer =
        Offer(
            offerId = "f1a5",
            campaignNo = "CMP-2026-000126",
            title = "25 GB Internet",
            score = 0.91,
            highlighted = true,
            status = "ACCEPTED",
            type = OfferType.ADD_ON
        )

    SubscriberHomeScreen(
        firstName = "Test",
        recommendedOffers = offers,
        latestAcceptedOffer = latestAcceptedOffer,
        onOfferClick = {},
        onCategoryClick = {},
        onHomeClick = {},
        onOffersClick = {},
        onProfileClick = {}
    )
}