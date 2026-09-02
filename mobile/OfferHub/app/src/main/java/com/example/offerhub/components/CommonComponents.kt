package com.example.offerhub.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.ui.theme.Primary
import com.example.offerhub.ui.theme.Secondary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomBarItem(
    val id: String,
    val icon: ImageVector,
    @StringRes val contentDescriptionRes: Int,
    val onClick: () -> Unit
)

@Composable
fun OfferHubTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .clip(
                RoundedCornerShape(
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            )
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Secondary, Primary)
                )
            )
            .statusBarsPadding()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OfferHubLogo(
                style =
                    OfferHubLogoStyle.THEME_AWARE,

                modifier = Modifier
                    .width(168.dp)
                    .height(56.dp)
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferHubDetailTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}

@Composable
fun AuthBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun OfferHubBottomBar(
    selectedItem: String,
    items: List<BottomBarItem>
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = selectedItem == item.id,
                onClick = item.onClick,
                colors = itemColors,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.contentDescriptionRes),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun SubscriberBottomBar(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onOffersClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    OfferHubBottomBar(
        selectedItem = selectedItem,
        items = listOf(
            BottomBarItem(
                id = "home",
                icon = Icons.Default.Home,
                contentDescriptionRes = R.string.nav_home,
                onClick = onHomeClick
            ),
            BottomBarItem(
                id = "offers",
                icon = Icons.Default.LocalOffer,
                contentDescriptionRes = R.string.nav_offers,
                onClick = onOffersClick
            ),
            BottomBarItem(
                id = "profile",
                icon = Icons.Default.Person,
                contentDescriptionRes = R.string.nav_profile,
                onClick = onProfileClick
            )
        )
    )
}

@Composable
fun NavigationActionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh,

            contentColor =
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = "$title ",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
fun SeeAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,

        modifier = modifier
            .width(55.dp)
            .height(30.dp),

        shape = RoundedCornerShape(50.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.view_all_offers),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

enum class OfferHubLogoStyle {
    THEME_AWARE,
    WHITE,
    GRADIENT
}
@Composable
fun OfferHubLogo(
    style: OfferHubLogoStyle,
    modifier: Modifier = Modifier
) {
    val painter =
        painterResource(
            id = R.drawable.offerhub_logo_vector
        )

    val tintColor =
        when (style) {
            OfferHubLogoStyle.THEME_AWARE ->
                MaterialTheme.colorScheme.onPrimary

            OfferHubLogoStyle.WHITE ->
                Color.White

            OfferHubLogoStyle.GRADIENT ->
                null
        }

    if (style == OfferHubLogoStyle.GRADIENT) {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.offerhub_logo_description),
            contentScale = ContentScale.Fit,

            modifier = modifier
                .graphicsLayer {
                    compositingStrategy =
                        CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val logoGradient =
                        Brush.horizontalGradient(
                            listOf(
                                Secondary,
                                Primary
                            )
                        )

                    onDrawWithContent {
                        drawContent()

                        drawRect(
                            brush = logoGradient,
                            blendMode = BlendMode.SrcIn
                        )
                    }
                }
        )
    } else {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.offerhub_logo_description),
            modifier = modifier,
            contentScale = ContentScale.Fit,

            colorFilter =
                ColorFilter.tint(
                    tintColor ?: Color.White
                )
        )
    }
}

