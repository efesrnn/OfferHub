package com.example.offerhub.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.offerhub.R

fun NavGraphBuilder.staffRoleGraphs() {
    composable(Routes.EXPERT_HOME) {
        RoleHomePlaceholder(stringResource(R.string.role_expert))
    }
    composable(Routes.SUPERVISOR_HOME) {
        RoleHomePlaceholder(stringResource(R.string.role_supervisor))
    }
    composable(Routes.ADMIN_HOME) {
        RoleHomePlaceholder(stringResource(R.string.role_admin))
    }
}

@Composable
private fun RoleHomePlaceholder(role: String) {
    Text(text = stringResource(R.string.role_home, role))
}
