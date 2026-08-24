package com.example.offerhub.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.R

@Composable
fun OfferCard(
    offer: Offer,
    onClick: () -> Unit,
    modifier: Modifier =
        Modifier.width(300.dp),
    isAccepted: Boolean = false,
    showStatus: Boolean = false,
    showRating: Boolean = false
) {
    Card(
        modifier = modifier
            .height(176.dp)
            .clickable(onClick = onClick),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh,

            contentColor =
                MaterialTheme.colorScheme.onSurface
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier.height(48.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = offer.title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onSurface,

                    maxLines = 2,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            Text(
                text = offer.campaignNo,
                fontSize = 13.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = 0.8f),

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (showRating && offer.rating != null) {
                Text(
                    text =
                        "★".repeat(offer.rating) +
                                "☆".repeat(5 - offer.rating),

                    fontSize = 18.sp,
                    color =
                        MaterialTheme.colorScheme.primary
                )
            } else {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    if (offer.highlighted) {
                        OfferTag(
                            text = stringResource(R.string.offer_recommended),

                            containerColor =
                                MaterialTheme.colorScheme.primaryContainer,

                            contentColor =
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (isAccepted) {
                        OfferTag(
                            text = stringResource(R.string.offer_accepted),

                            containerColor =
                                MaterialTheme.colorScheme.tertiaryContainer,

                            contentColor =
                                MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    if (showStatus) {
                        when (offer.status) {

                            OfferStatus.ACCEPTED -> {
                                OfferTag(
                                    text = stringResource(R.string.offer_accepted),
                                    containerColor =
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor =
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            OfferStatus.DECLINED -> {
                                OfferTag(
                                    text = stringResource(R.string.offer_not_interested),
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OfferStatus.PENDING -> {
                                OfferTag(
                                    text = stringResource(R.string.offer_pending),
                                    containerColor =
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor =
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferTag(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,

            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
        )
    }
}
