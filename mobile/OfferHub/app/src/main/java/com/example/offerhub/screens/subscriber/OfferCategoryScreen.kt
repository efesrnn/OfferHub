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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.components.OfferCard
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.data.model.Offer
import com.example.offerhub.R

@Composable
fun OfferCategoryScreen(
    title: String,
    offers: List<Offer>,
    onBackClick: () -> Unit,
    onOfferClick: (String) -> Unit,
    showAcceptedTag: Boolean = false,
    ratings: Map<String, Int> = emptyMap(),
    emptyMessage: String? = null
) {
    val resolvedEmptyMessage =
        emptyMessage ?: stringResource(R.string.offers_category_empty)
    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background,

        contentColor =
            MaterialTheme.colorScheme.onBackground,

        topBar = {
            OfferHubTopBar()
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TextButton(
                onClick = onBackClick,
                modifier =
                    Modifier.padding(horizontal = 12.dp)
            ) {
                Text(text = stringResource(R.string.back))
            }

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onBackground,

                modifier =
                    Modifier.padding(horizontal = 24.dp)
            )

            if (offers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                    text = resolvedEmptyMessage,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(
                        horizontal = 24.dp,
                        vertical = 20.dp
                    ),

                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
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
                                Modifier.fillMaxWidth(),

                            isAccepted =
                                showAcceptedTag,

                            showRating =
                                ratings.containsKey(offer.offerId),

                            onClick = {
                                onOfferClick(
                                    offer.offerId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
