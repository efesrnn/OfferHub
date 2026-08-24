package com.example.offerhub.screens.subscriber

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.example.offerhub.components.SeeAllButton
import com.example.offerhub.data.model.OfferStatus

@Composable
fun OffersScreen(
    offers: List<Offer>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetryClick: () -> Unit,
    onOfferClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onOffersClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAcceptedOffersClick: () -> Unit,
    onRatedOffersClick: () -> Unit,
    onSeeAllClick: (OfferType) -> Unit,
) {
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
                selectedItem = "offers",
                onHomeClick = onHomeClick,
                onOffersClick = onOffersClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->

        when {
            isLoading -> {
                OffersLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            errorMessage != null -> {
                OffersErrorState(
                    message = errorMessage,
                    onRetryClick = onRetryClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            offers.isEmpty() -> {
                OffersEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                OffersContent(
                    offers = offers,
                    onOfferClick = onOfferClick,
                    onSeeAllClick = onSeeAllClick,
                    onAcceptedOffersClick =
                        onAcceptedOffersClick,
                    onRatedOffersClick =
                        onRatedOffersClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun OffersContent(
    offers: List<Offer>,
    onOfferClick: (String) -> Unit,
    onSeeAllClick: (OfferType) -> Unit,
    onAcceptedOffersClick: () -> Unit,
    onRatedOffersClick: () -> Unit,
    modifier: Modifier = Modifier
){
    val addOnOffers = offers.filter { offer ->
        offer.type == OfferType.ADD_ON &&
                offer.status == OfferStatus.PENDING
    }

    val tariffOffers = offers.filter { offer ->
        offer.type == OfferType.TARIFF_UPGRADE &&
                offer.status == OfferStatus.PENDING
    }

    val deviceOffers = offers.filter { offer ->
        offer.type == OfferType.DEVICE_OFFER &&
                offer.status == OfferStatus.PENDING
    }

    val loyaltyOffers = offers.filter { offer ->
        offer.type == OfferType.LOYALTY &&
                offer.status == OfferStatus.PENDING
    }
    LazyColumn(
        modifier = modifier,

        contentPadding = PaddingValues(
            vertical = 20.dp
        ),

        verticalArrangement =
            Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(
                modifier =
                    Modifier.padding(horizontal = 24.dp),

                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Offers",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Offers selected for you",
                    fontSize = 15.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            OfferCategorySection(
                title = "Add-on Packages",
                offers = addOnOffers,
                onOfferClick = onOfferClick,
                onSeeAllClick = {
                    onSeeAllClick(OfferType.ADD_ON)
                }
            )
        }

        item {
            OfferCategorySection(
                title = "Tariff Upgrades",
                offers = tariffOffers,
                onOfferClick = onOfferClick,
                onSeeAllClick = {
                    onSeeAllClick(OfferType.TARIFF_UPGRADE)
                }
            )
        }

        item {
            OfferCategorySection(
                title = "Device Offers",
                offers = deviceOffers,
                onOfferClick = onOfferClick,
                onSeeAllClick = {
                    onSeeAllClick(OfferType.DEVICE_OFFER)
                }
            )
        }

        item {
            OfferCategorySection(
                title = "Loyalty Offers",
                offers = loyaltyOffers,
                onOfferClick = onOfferClick,
                onSeeAllClick = {
                    onSeeAllClick(OfferType.LOYALTY)
                }
            )
        }

        item {
            Text(
                text = "My Offers",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color =
                    MaterialTheme.colorScheme.onBackground,

                modifier =
                    Modifier.padding(horizontal = 24.dp)
            )
        }

        item {
            Column(
                modifier =
                    Modifier.padding(horizontal = 24.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                NavigationActionCard(
                    title = "My Accepted Offers",
                    onClick = onAcceptedOffersClick
                )

                NavigationActionCard(
                    title = "My Rated Offers",
                    onClick = onRatedOffersClick
                )
            }
        }
    }
}

@Composable
private fun OffersLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun OffersErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )

        Button(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Try Again")
        }
    }
}

@Composable
private fun OffersEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "No offers available",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "New personalized offers will appear here.",
            modifier = Modifier.padding(top = 8.dp),
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OfferCategorySection(
    title: String,
    offers: List<Offer>,
    onOfferClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (offers.isEmpty()) {
            Text(
                text = "No offers available in this category.",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                modifier =
                    Modifier.padding(horizontal = 24.dp)
            )
        } else {
            LazyRow(
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 24.dp
                    ),

                horizontalArrangement =
                    Arrangement.spacedBy(16.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                items(
                    items = offers,
                    key = { offer ->
                        offer.offerId
                    }
                ) { offer ->
                    OfferCard(
                        offer = offer,

                        modifier =
                            Modifier.width(300.dp),

                        onClick = {
                            onOfferClick(offer.offerId)
                        }
                    )
                }

                item {
                    SeeAllButton(
                        onClick = onSeeAllClick
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OffersScreenPreview() {
    OffersScreen(
        offers = listOf(
            Offer(
                offerId = "f1a2",
                campaignNo = "CMP-2026-000123",
                title = "20 GB Internet",
                score = 0.83,
                highlighted = true,
                status = OfferStatus.PENDING,
                type = OfferType.ADD_ON
            ),

            Offer(
                offerId = "f1a3",
                campaignNo = "CMP-2026-000124",
                title = "Social Media Plus",
                score = 0.76,
                highlighted = false,
                status = OfferStatus.ACCEPTED,
                type = OfferType.ADD_ON
            )
        ),

        onRetryClick = {},
        onOfferClick = {},
        onHomeClick = {},
        onOffersClick = {},
        onProfileClick = {},
        onRatedOffersClick = {},
        onAcceptedOffersClick = {},
        onSeeAllClick = {},
    )
}