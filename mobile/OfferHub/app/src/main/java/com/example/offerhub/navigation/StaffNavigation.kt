package com.example.offerhub.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.offerhub.R
import com.example.offerhub.screens.admin.AdminHomeScreen
import com.example.offerhub.screens.admin.AdminProfileScreen
import com.example.offerhub.screens.admin.AuditLogsScreen
import com.example.offerhub.screens.admin.CreateStaffScreen
import com.example.offerhub.screens.admin.SearchStaffScreen
import com.example.offerhub.screens.admin.UpdateStaffRoleScreen
import com.example.offerhub.viewModel.AuthViewModel
import com.example.offerhub.viewModel.AdminViewModel

fun NavGraphBuilder.staffRoleGraphs(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel
) {
    composable(Routes.EXPERT_HOME) {
        Text(text = stringResource(R.string.role_home, stringResource(R.string.role_expert)))
    }
    composable(Routes.SUPERVISOR_HOME) {
        Text(text = stringResource(R.string.role_home, stringResource(R.string.role_supervisor)))
    }
    composable(Routes.ADMIN_HOME) {
        AdminHomeScreen(
            onCreateStaffClick = {
                navController.navigate(Routes.ADMIN_CREATE_STAFF)
            },
            onSearchStaffClick = {
                navController.navigate(Routes.ADMIN_SEARCH_STAFF)
            },
            onUpdateRoleClick = {
                navController.navigate(Routes.ADMIN_UPDATE_ROLE)
            },
            onAuditLogsClick = {
                navController.navigate(Routes.ADMIN_AUDIT_LOGS)
            },
            onProfileClick = {
                navController.navigateAdminTopLevel(Routes.ADMIN_PROFILE)
            }
        )
    }

    composable(Routes.ADMIN_CREATE_STAFF) {
        val adminState by adminViewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { adminViewModel.clearActionFeedback() }
        CreateStaffScreen(
            onBackClick = navController::popBackStack,
            onCreateStaff = adminViewModel::createStaff,
            onClearClick = adminViewModel::clearActionFeedback,
            isSubmitting = adminState.isSubmitting,
            successMessage = adminState.actionMessage,
            createdStaffId = adminState.createdStaffId,
            errorMessage = adminState.actionError
        )
    }

    composable(Routes.ADMIN_SEARCH_STAFF) {
        val adminState by adminViewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { adminViewModel.clearStaffSearch() }
        SearchStaffScreen(
            query = adminState.staffSearchQuery,
            results = adminState.staffSearchResults,
            isLoading = adminState.isSearchingStaff,
            errorMessage = adminState.staffSearchError,
            onBackClick = navController::popBackStack,
            onQueryChange = adminViewModel::onStaffSearchQueryChange,
            onClearClick = adminViewModel::clearStaffSearch
        )
    }

    composable(Routes.ADMIN_UPDATE_ROLE) {
        val adminState by adminViewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { adminViewModel.clearActionFeedback() }
        UpdateStaffRoleScreen(
            staffId = adminState.staffIdQuery,
            selectedStaff = adminState.selectedStaff,
            isLookingUpStaff = adminState.isLookingUpStaff,
            staffLookupError = adminState.staffLookupError,
            onBackClick = navController::popBackStack,
            onStaffIdChange = adminViewModel::onStaffIdChange,
            onUpdateRole = adminViewModel::updateRole,
            onClearClick = adminViewModel::clearStaffLookup,
            isSubmitting = adminState.isSubmitting,
            successMessage = adminState.actionMessage,
            errorMessage = adminState.actionError
        )
    }

    composable(Routes.ADMIN_AUDIT_LOGS) {
        val adminState by adminViewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { adminViewModel.loadAuditLogs(reset = true) }
        AuditLogsScreen(
            logs = adminState.auditLogs,
            actionQuery = adminState.actionQuery.orEmpty(),
            selectedAction = adminState.auditAction,
            selectedResult = adminState.auditResult,
            selectedFromDate = adminState.fromDate,
            selectedToDate = adminState.toDate,
            onBackClick = navController::popBackStack,
            onActionQueryChange = adminViewModel::onActionQueryChange,
            onApplyFilters = adminViewModel::applyAuditFilters,
            onClearFilters = adminViewModel::clearAuditFilters,
            onLoadNextPage = { adminViewModel.loadAuditLogs(reset = false) },
            onRetryClick = { adminViewModel.loadAuditLogs(reset = true) },
            isLoading = adminState.isLoadingAudit,
            isLoadingNextPage = adminState.isLoadingNextAuditPage,
            canLoadMore = adminState.canLoadMoreAudit,
            errorMessage = adminState.auditError
        )
    }

    composable(Routes.ADMIN_PROFILE) {
        val authState by authViewModel.uiState.collectAsState()
        val user = authState.currentUser

        AdminProfileScreen(
            userId = user?.id.orEmpty(),
            role = user?.role ?: "ADMIN",
            onLogoutClick = {
                navController.navigate(Routes.AUTH_CHOICE) {
                    popUpTo(Routes.ADMIN_HOME) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.logout()
            },
            onHomeClick = {
                navController.navigateAdminTopLevel(Routes.ADMIN_HOME)
            }
        )
    }
}

private fun NavHostController.navigateAdminTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.ADMIN_HOME)
        launchSingleTop = true
    }
}
