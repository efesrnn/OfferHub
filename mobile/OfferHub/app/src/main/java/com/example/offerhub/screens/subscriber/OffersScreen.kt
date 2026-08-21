package com.example.offerhub.screens.subscriber

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.offerhub.components.OfferCard
import com.example.offerhub.components.OfferHubBottomBar
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.data.model.Offer

@Composable
fun OffersScreen(
    offers: List<Offer>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetryClick: () -> Unit,
    onOfferClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onOffersClick: () -> Unit,
    onProfileClick: () -> Unit
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,

        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 20.dp
        ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Your Offers",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Text(
                text = "Offers selected for you",
                fontSize = 15.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(
            items = offers,
            key = { offer ->
                offer.offerId
            }
        ) { offer ->
            OfferCard(
                offer = offer,

                modifier =
                    Modifier.fillMaxWidth(),

                showStatus = true,

                onClick = {
                    onOfferClick(offer.offerId)
                }
            )
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
                status = "PENDING"
            ),

            Offer(
                offerId = "f1a3",
                campaignNo = "CMP-2026-000124",
                title = "Social Media Plus",
                score = 0.76,
                highlighted = false,
                status = "ACCEPTED"
            )
        ),

        onRetryClick = {},
        onOfferClick = {},
        onHomeClick = {},
        onOffersClick = {},
        onProfileClick = {}
    )
}