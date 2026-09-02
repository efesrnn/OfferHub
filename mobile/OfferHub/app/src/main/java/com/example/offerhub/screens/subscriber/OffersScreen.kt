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
import com.example.offerhub.data.model.OfferType
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.example.offerhub.components.SeeAllButton
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.R
import com.example.offerhub.data.mock.MockOfferData
import com.example.offerhub.ui.theme.OfferHubTheme

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
            SubscriberBottomBar(
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
                    text = stringResource(R.string.nav_offers),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = stringResource(R.string.offers_subtitle),
                    fontSize = 15.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            OfferCategorySection(
                title = stringResource(R.string.offers_add_on_packages),
                offers = addOnOffers,
                onOfferClick = onOfferClick,
                onSeeAllClick = {
                    onSeeAllClick(OfferType.ADD_ON)
                }
            )
        }

        item {
            OfferCategorySection(
                title = stringResource(R.string.offers_tariff_upgrades),
                offers = tariffOffers,
                onOfferClick = onOfferClick,
                onSeeAllClick = {
                    onSeeAllClick(OfferType.TARIFF_UPGRADE)
                }
            )
        }

        item {
            OfferCategorySection(
                title = stringResource(R.string.offers_device),
                offers = deviceOffers,
                onOfferClick = onOfferClick,
                onSeeAllClick = {
                    onSeeAllClick(OfferType.DEVICE_OFFER)
                }
            )
        }

        item {
            OfferCategorySection(
                title = stringResource(R.string.offers_loyalty),
                offers = loyaltyOffers,
                onOfferClick = onOfferClick,
                onSeeAllClick = {
                    onSeeAllClick(OfferType.LOYALTY)
                }
            )
        }

        item {
            Text(
                text = stringResource(R.string.offers_my_offers),
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
                    title = stringResource(R.string.offers_my_accepted),
                    onClick = onAcceptedOffersClick
                )

                NavigationActionCard(
                    title = stringResource(R.string.offers_my_rated),
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
            Text(text = stringResource(R.string.offers_try_again))
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
            text = stringResource(R.string.offers_empty_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.offers_empty_message),
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
                text = stringResource(R.string.offers_category_empty),
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
    // TODO: Remove temporary subscriber previews after real backend integration is testable.
    OfferHubTheme {
        OffersScreen(
            offers = MockOfferData.offers,
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
}
