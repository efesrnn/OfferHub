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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.data.model.Campaign
import com.example.offerhub.data.model.Offer
import com.example.offerhub.ui.theme.Primary
import com.example.offerhub.ui.theme.Secondary

@Composable
fun OfferCard(
    offer: Offer,
    onClick: () -> Unit,
    isAccepted: Boolean = false,
    rating: Int? = null
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(150.dp)
            .clickable(onClick = onClick),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh,

            contentColor =
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = offer.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = offer.campaignNo,
                fontSize = 13.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.75f
                    )
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (offer.highlighted) {
                Text(
                    text = "Recommended",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isAccepted) {
                Text(
                    text = "Accepted",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (rating != null) {
                Text(
                    text =
                        "★".repeat(rating) +
                                "☆".repeat(5 - rating),

                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}