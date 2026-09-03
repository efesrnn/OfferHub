package com.example.offerhub.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import com.example.offerhub.R

@Composable
fun SupervisorBottomBar(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onOperationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    OfferHubBottomBar(
        selectedItem = selectedItem,
        items = listOf(
            BottomBarItem("home", Icons.Default.Home, R.string.expert_home, onHomeClick),
            BottomBarItem("operations", Icons.Default.Work, R.string.supervisor_operations, onOperationsClick),
            BottomBarItem("profile", Icons.Default.Person, R.string.nav_profile, onProfileClick)
        )
    )
}
