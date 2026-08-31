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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.model.OfferType
import com.example.offerhub.R
import com.example.offerhub.ui.format.formatOfferTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDetailBottomSheet(
    offer: Offer,
    isSubmitting: Boolean = false,
    actionError: String? = null,
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
                label = stringResource(R.string.offer_type_label),
                value = offer.type.displayName()
            )

            offer.discountRate?.let { discountRate ->
                DetailRow(
                    label = stringResource(R.string.offer_discount_label),
                    value = "${discountRate.toDisplayNumber()}%"
                )
            }

            offer.validUntil?.let { validUntil ->
                DetailRow(
                    label = stringResource(R.string.offer_valid_until_label),
                    value = formatOfferTimestamp(validUntil)
                )
            }

            DetailRow(
                label = stringResource(R.string.offer_status_label),
                value = offer.status.displayName()
            )

            offer.acceptedAt?.let { acceptedAt ->
                DetailRow(
                    label = stringResource(R.string.offer_accepted_on_label),
                    value = formatOfferTimestamp(acceptedAt)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            actionError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

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
                            modifier = Modifier.weight(1f),
                            enabled = !isSubmitting
                        ) {
                            Text(text = stringResource(R.string.offer_not_interested))
                        }

                        Button(
                            onClick = {
                                onAcceptClick(offer.offerId)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(text = stringResource(R.string.offer_accept))
                            }
                        }
                    }
                }

                OfferStatus.ACCEPTED -> {
                    Text(
                        text = stringResource(R.string.offer_rate_experience),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.offer_rate_later))
                        }

                        Button(
                            onClick = {
                                onSubmitRating(
                                    offer.offerId,
                                    selectedRating
                                )
                            },
                            enabled = selectedRating in 1..5 && !isSubmitting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text =
                                    if (offer.rating == null) {
                                        stringResource(R.string.offer_submit_rating)
                                    } else {
                                        stringResource(R.string.offer_update_rating)
                                    }
                            )
                        }
                    }
                }

                OfferStatus.DECLINED -> {
                    Text(
                        text =
                            stringResource(R.string.offer_declined_message),
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

@Composable
private fun OfferStatus.displayName(): String = stringResource(
    when (this) {
        OfferStatus.PENDING -> R.string.offer_available
        OfferStatus.ACCEPTED -> R.string.offer_accepted
        OfferStatus.DECLINED -> R.string.offer_not_interested
    }
)

@Composable
private fun OfferType.displayName(): String = stringResource(
    when (this) {
        OfferType.ADD_ON -> R.string.offer_type_add_on
        OfferType.TARIFF_UPGRADE -> R.string.offer_type_tariff
        OfferType.DEVICE_OFFER -> R.string.offer_type_device
        OfferType.LOYALTY -> R.string.offer_type_loyalty
    }
)

private fun Double.toDisplayNumber(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}
