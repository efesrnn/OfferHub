package com.example.offerhub.components

import android.R.attr.onClick
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.data.model.Campaign
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.ui.theme.Primary
import com.example.offerhub.ui.theme.Secondary
@Composable
fun OfferCard(
    offer: Offer,
    onClick: () -> Unit,
    modifier: Modifier =
        Modifier.width(300.dp),
    isAccepted: Boolean = false,
    rating: Int? = null,
    showStatus: Boolean = false
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

            if (rating != null) {
                Text(
                    text =
                        "★".repeat(rating) +
                                "☆".repeat(5 - rating),

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
                            text = "Recommended",

                            containerColor =
                                MaterialTheme.colorScheme.primaryContainer,

                            contentColor =
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (isAccepted) {
                        OfferTag(
                            text = "Accepted",

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
                                    text = "Accepted",
                                    containerColor =
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor =
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            OfferStatus.DECLINED -> {
                                OfferTag(
                                    text = "Not interested",
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OfferStatus.PENDING -> {
                                OfferTag(
                                    text = "Pending",
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