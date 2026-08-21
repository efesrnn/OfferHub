package com.example.offerhub.screens.subscriber

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.components.OfferCard
import com.example.offerhub.components.OfferHubBottomBar
import com.example.offerhub.components.OfferHubTopBar
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.RatedOffer


@Composable
fun SubscriberHomeScreen(
    firstName: String,
    recommendedOffers: List<Offer>,
    acceptedOffers: List<Offer>,
    ratedOffers: List<RatedOffer>,
    onOfferClick: (String) -> Unit
) {

    val recommendedState = rememberLazyListState()
    val acceptedState = rememberLazyListState()
    val ratedState = rememberLazyListState()

    val recommendedFling = rememberSnapFlingBehavior(
        lazyListState = recommendedState
    )

    val acceptedFling = rememberSnapFlingBehavior(
        lazyListState = acceptedState
    )

    val ratedFling = rememberSnapFlingBehavior(
        lazyListState = ratedState
    )

    Scaffold(
        topBar = {
            OfferHubTopBar()
        },

        bottomBar = {
            OfferHubBottomBar(
                selectedItem = "home",
                onHomeClick = {
                    // zaten Home ekranındayız
                },
                onOffersClick = {
                    // Offers navigation sonra bağlanacak
                },
                onProfileClick = {
                    // Profile navigation sonra bağlanacak
                }
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


            // ACCEPTED OFFERS
            item {
                Text(
                    text = "Accepted campaigns",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                LazyRow(
                    state = acceptedState,
                    flingBehavior = acceptedFling,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    items(acceptedOffers) { offer ->

                        OfferCard(
                            offer = offer,
                            onClick = {
                                onOfferClick(offer.offerId)
                            },
                            isAccepted = true
                        )
                    }
                }
            }


            // RECENTLY RATED
            item {
                Text(
                    text = "Recently rated",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                LazyRow(
                    state = ratedState,
                    flingBehavior = ratedFling,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    items(ratedOffers) { ratedOffer ->

                        OfferCard(
                            offer = ratedOffer.offer,
                            onClick = {
                                onOfferClick(
                                    ratedOffer.offer.offerId
                                )
                            },
                            rating = ratedOffer.rating
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun SubscriberHomeScreenPreview() {

    val offers = listOf(
        Offer(
            offerId = "f1a2",
            campaignNo = "CMP-2026-000123",
            title = "Summer Extra Package",
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
            status = "PENDING"
        ),

        Offer(
            offerId = "f1a4",
            campaignNo = "CMP-2026-000125",
            title = "Weekend Internet",
            score = 0.68,
            highlighted = false,
            status = "PENDING"
        )
    )

    val acceptedOffers = listOf(
        Offer(
            offerId = "f1a5",
            campaignNo = "CMP-2026-000126",
            title = "25 GB Internet",
            score = 0.91,
            highlighted = true,
            status = "ACCEPTED"
        )
    )

    val ratedOffers = listOf(
        RatedOffer(
            offer = acceptedOffers[0],
            rating = 4
        )
    )

    SubscriberHomeScreen(
        firstName = "A",
        recommendedOffers = offers,
        acceptedOffers = acceptedOffers,
        ratedOffers = ratedOffers,
        onOfferClick = {}
    )
}