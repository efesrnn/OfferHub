package com.example.offerhub.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.offerhub.R

@Composable
fun SupervisorBottomBar(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onOperationsClick: () -> Unit,
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
        NavigationBarItem(selected = selectedItem == "home", onClick = onHomeClick, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.expert_home)) }, colors = colors)
        NavigationBarItem(selected = selectedItem == "operations", onClick = onOperationsClick, icon = { Icon(Icons.Default.Work, null) }, label = { Text(stringResource(R.string.supervisor_operations)) }, colors = colors)
        NavigationBarItem(selected = selectedItem == "profile", onClick = onProfileClick, icon = { Icon(Icons.Default.Person, null) }, label = { Text(stringResource(R.string.nav_profile)) }, colors = colors)
    }
}
