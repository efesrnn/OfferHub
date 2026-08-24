package com.example.offerhub.screens.subscriber

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.model.OfferType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDetailBottomSheet(
    offer: Offer,
    onDismiss: () -> Unit,
    onAcceptClick: (String) -> Unit,
    onDeclineClick: (String) -> Unit,
    onSubmitRating: (offerId: String, rating: Int) -> Unit
) {
    var selectedRating by remember(offer.offerId, offer.rating) {
        mutableIntStateOf(offer.rating ?: 0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = 24.dp,
                    vertical = 12.dp
                )
        ) {
            Text(
                text = offer.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = offer.campaignNo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (offer.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = offer.description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            DetailRow(
                label = "Offer type",
                value = offer.type.displayName()
            )

            offer.discountRate?.let { discountRate ->
                DetailRow(
                    label = "Discount",
                    value = "${discountRate.toDisplayNumber()}%"
                )
            }

            offer.validUntil?.let { validUntil ->
                DetailRow(
                    label = "Valid until",
                    value = validUntil
                )
            }

            DetailRow(
                label = "Status",
                value = offer.status.displayName()
            )

            offer.acceptedAt?.let { acceptedAt ->
                DetailRow(
                    label = "Accepted on",
                    value = acceptedAt
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (offer.status) {
                OfferStatus.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDeclineClick(offer.offerId)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Not interested")
                        }

                        Button(
                            onClick = {
                                onAcceptClick(offer.offerId)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Accept")
                        }
                    }
                }

                OfferStatus.ACCEPTED -> {
                    Text(
                        text = "Rate your experience",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.Center,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        for (ratingValue in 1..5) {
                            Text(
                                text =
                                    if (ratingValue <= selectedRating) {
                                        "★"
                                    } else {
                                        "☆"
                                    },
                                fontSize = 38.sp,
                                color =
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        selectedRating = ratingValue
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onSubmitRating(
                                offer.offerId,
                                selectedRating
                            )
                        },
                        enabled = selectedRating in 1..5,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text =
                                if (offer.rating == null) {
                                    "Submit Rating"
                                } else {
                                    "Update Rating"
                                }
                        )
                    }
                }

                OfferStatus.DECLINED -> {
                    Text(
                        text =
                            "You marked this offer as not interested.",
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun OfferStatus.displayName(): String {
    return when (this) {
        OfferStatus.PENDING -> "Available"
        OfferStatus.ACCEPTED -> "Accepted"
        OfferStatus.DECLINED -> "Not interested"
    }
}

private fun OfferType.displayName(): String {
    return when (this) {
        OfferType.ADD_ON -> "Add-on Package"
        OfferType.TARIFF_UPGRADE -> "Tariff Upgrade"
        OfferType.DEVICE_OFFER -> "Device Offer"
        OfferType.LOYALTY -> "Loyalty Offer"
    }
}

private fun Double.toDisplayNumber(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}