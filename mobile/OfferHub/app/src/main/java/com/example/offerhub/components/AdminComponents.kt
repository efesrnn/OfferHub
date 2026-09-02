package com.example.offerhub.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.offerhub.R

@Composable
fun AdminBottomBar(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = selectedItem == "home",
            onClick = onHomeClick,
            colors = colors,
            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.admin_home)) }
        )
        NavigationBarItem(
            selected = selectedItem == "profile",
            onClick = onProfileClick,
            colors = colors,
            icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.admin_profile_nav)) }
        )
    }
}
