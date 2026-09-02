package com.example.offerhub.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import com.example.offerhub.R

@Composable
fun AdminBottomBar(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    OfferHubBottomBar(
        selectedItem = selectedItem,
        items = listOf(
            BottomBarItem("home", Icons.Default.Home, R.string.admin_home, onHomeClick),
            BottomBarItem("profile", Icons.Default.Person, R.string.admin_profile_nav, onProfileClick)
        )
    )
}
