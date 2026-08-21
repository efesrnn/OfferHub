package com.example.offerhub.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.ui.theme.Primary
import com.example.offerhub.ui.theme.Secondary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.graphics.Color

@Composable
fun OfferHubTopBar(
    title: String = "OfferHub"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
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
           Row(
               verticalAlignment = Alignment.CenterVertically,
           ) {
               Image(
                   painter = painterResource(R.drawable.logo_white),
                   contentDescription = "OfferHub Logo",
                   modifier = Modifier.size(50.dp)
               )
               Spacer(modifier=Modifier.width(3.dp))
               Text(
                   text = title,
                   fontSize = 27.sp,
                   fontWeight = FontWeight.Bold,
                   color = Color.White
               )
           }
        }

    }
}

@Composable
fun OfferHubBottomBar(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onOffersClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {

        NavigationBar(
            modifier = Modifier.fillMaxWidth()
        ) {

            NavigationBarItem(
                selected = selectedItem == "home",
                onClick = onHomeClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home"
                    )
                },
                label = {
                    Text("Home")
                }
            )

            NavigationBarItem(
                selected = selectedItem == "offers",
                onClick = onOffersClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = "Offers"
                    )
                },
                label = {
                    Text("Offers")
                }
            )

            NavigationBarItem(
                selected = selectedItem == "profile",
                onClick = onProfileClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile"
                    )
                },
                label = {
                    Text("Profile")
                }
            )
        }
    }
}

