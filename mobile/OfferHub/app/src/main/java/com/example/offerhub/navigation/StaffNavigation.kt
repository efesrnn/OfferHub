package com.example.offerhub.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.offerhub.R
import com.example.offerhub.screens.admin.AdminHomeScreen
import com.example.offerhub.screens.admin.AdminProfileScreen
import com.example.offerhub.screens.admin.AuditLogsScreen
import com.example.offerhub.screens.admin.CreateStaffScreen
import com.example.offerhub.screens.admin.SearchStaffScreen
import com.example.offerhub.screens.admin.UpdateStaffRoleScreen
import com.example.offerhub.screens.expert.ExpertCaseListScreen
import com.example.offerhub.screens.expert.ExpertCaseDetailScreen
import com.example.offerhub.screens.expert.ExpertHomeScreen
import com.example.offerhub.screens.expert.ExpertProfileScreen
import com.example.offerhub.screens.expert.ExpertCampaignListScreen
import com.example.offerhub.screens.expert.ExpertCampaignDetailScreen
import com.example.offerhub.screens.expert.CreateCampaignScreen
import com.example.offerhub.screens.expert.ExpertOperationsScreen
import com.example.offerhub.ui.text.asString
import com.example.offerhub.viewModel.AuthViewModel
import com.example.offerhub.viewModel.AdminViewModel
import com.example.offerhub.viewModel.ExpertViewModel

fun NavGraphBuilder.staffRoleGraphs(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    expertViewModel: ExpertViewModel
) {
    composable(Routes.EXPERT_HOME) {
        val expertState by expertViewModel.uiState.collectAsState()
        LaunchedEffect(Unit) {
            if (expertState.caseStatusFilter != null) {
                expertViewModel.loadCases(reset = true, status = null)
            }
        }
        ExpertHomeScreen(
            cases = expertState.cases,
            isLoading = expertState.isLoading,
            errorMessage = expertState.errorMessage?.asString(),
            onRetryClick = { expertViewModel.loadCases(reset = true) },
            onCaseClick = { caseId ->
                navController.navigate(Routes.expertCaseDetail(caseId))
            },
            onOperationsClick = { navController.navigateExpertTopLevel(Routes.EXPERT_OPERATIONS) },
            onCriticalCasesClick = { navController.navigateExpertTopLevel(Routes.EXPERT_CRITICAL_CASES) },
            onProfileClick = { navController.navigateExpertTopLevel(Routes.EXPERT_PROFILE) }
        )
    }
    composable(Routes.EXPERT_OPERATIONS) {
        ExpertOperationsScreen(
            onCasesClick = { navController.navigate(Routes.EXPERT_CASES) },
            onCampaignsClick = { navController.navigate(Routes.EXPERT_CAMPAIGNS) },
            onHomeClick = { navController.navigateExpertTopLevel(Routes.EXPERT_HOME) },
            onProfileClick = { navController.navigateExpertTopLevel(Routes.EXPERT_PROFILE) }
        )
    }
    composable(Routes.EXPERT_CASES) {
        val expertState by expertViewModel.uiState.collectAsState()
        ExpertCaseListScreen(
            cases = expertState.cases,
            isLoading = expertState.isLoading,
            isLoadingNextPage = expertState.isLoadingNextPage,
            canLoadMore = expertState.canLoadMore,
            errorMessage = expertState.errorMessage?.asString(),
            initialCriticalOnly = false,
            initialStatusFilter = expertState.caseStatusFilter,
            onRetryClick = { expertViewModel.loadCases(reset = true) },
            onLoadNextPage = { expertViewModel.loadCases(reset = false) },
            onStatusFilterChanged = { status -> expertViewModel.loadCases(reset = true, status = status) },
            onCaseClick = { caseId ->
                navController.navigate(Routes.expertCaseDetail(caseId))
            },
            onBackClick = navController::popBackStack,
            onHomeClick = { navController.navigateExpertTopLevel(Routes.EXPERT_HOME) },
            onOperationsClick = { navController.navigateExpertTopLevel(Routes.EXPERT_OPERATIONS) },
            onProfileClick = { navController.navigateExpertTopLevel(Routes.EXPERT_PROFILE) }
        )
    }
    composable(Routes.EXPERT_CRITICAL_CASES) {
        val expertState by expertViewModel.uiState.collectAsState()
        ExpertCaseListScreen(
            cases = expertState.cases,
            isLoading = expertState.isLoading,
            isLoadingNextPage = expertState.isLoadingNextPage,
            canLoadMore = expertState.canLoadMore,
            errorMessage = expertState.errorMessage?.asString(),
            initialCriticalOnly = true,
            initialStatusFilter = expertState.caseStatusFilter,
            onRetryClick = { expertViewModel.loadCases(reset = true) },
            onLoadNextPage = { expertViewModel.loadCases(reset = false) },
            onStatusFilterChanged = { status -> expertViewModel.loadCases(reset = true, status = status) },
            onCaseClick = { caseId -> navController.navigate(Routes.expertCaseDetail(caseId)) },
            onBackClick = navController::popBackStack,
            onHomeClick = { navController.navigateExpertTopLevel(Routes.EXPERT_HOME) },
            onOperationsClick = { navController.navigateExpertTopLevel(Routes.EXPERT_OPERATIONS) },
            onProfileClick = { navController.navigateExpertTopLevel(Routes.EXPERT_PROFILE) }
        )
    }
    composable(Routes.EXPERT_PROFILE) {
        val authState by authViewModel.uiState.collectAsState()
        val profileUser = remember { authState.currentUser }
        ExpertProfileScreen(
            userId = profileUser?.id.orEmpty(),
            role = profileUser?.role ?: "EXPERT",
            specialties = profileUser?.specialties.orEmpty(),
            regions = profileUser?.regions.orEmpty(),
            onLogoutClick = {
                navController.navigate(Routes.AUTH_CHOICE) {
                    popUpTo(Routes.EXPERT_HOME) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.logout()
            },
            onHomeClick = { navController.navigateExpertTopLevel(Routes.EXPERT_HOME) },
            onOperationsClick = { navController.navigateExpertTopLevel(Routes.EXPERT_OPERATIONS) }
        )
    }
    composable(Routes.EXPERT_CAMPAIGNS) {
        val expertState by expertViewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { expertViewModel.loadCampaigns() }
        ExpertCampaignListScreen(
            campaigns = expertState.campaigns,
            isLoading = expertState.isLoadingCampaigns,
            isLoadingNextPage = expertState.isLoadingNextCampaignPage,
            canLoadMore = expertState.canLoadMoreCampaigns,
            errorMessage = expertState.campaignErrorMessage?.asString(),
            selectedStatus = expertState.campaignStatusFilter,
            selectedSegment = expertState.campaignSegmentFilter,
            onBackClick = navController::popBackStack,
            onRetryClick = { expertViewModel.loadCampaigns() },
            onCreateClick = { navController.navigate(Routes.EXPERT_CREATE_CAMPAIGN) },
            onLoadNextPage = { expertViewModel.loadCampaigns(reset = false) },
            onApplyFilters = { status, segment ->
                expertViewModel.loadCampaigns(reset = true, status = status, segment = segment)
            },
            onCampaignClick = { campaignNo ->
                navController.navigate(Routes.expertCampaignDetail(campaignNo))
            }
        )
    }
    composable(Routes.EXPERT_CREATE_CAMPAIGN) {
        val expertState by expertViewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { expertViewModel.clearCampaignFeedback() }
        CreateCampaignScreen(
            isSubmitting = expertState.isCreatingCampaign,
            errorMessage = expertState.campaignActionError?.asString(),
            createdCampaignNo = expertState.createdCampaignNo,
            onBackClick = navController::popBackStack,
            onCreate = expertViewModel::createCampaign
        )
    }
    composable(
        route = Routes.EXPERT_CAMPAIGN_DETAIL_WITH_NO,
        arguments = listOf(navArgument("campaignNo") { type = NavType.StringType })
    ) { backStackEntry ->
        val campaignNo = backStackEntry.arguments?.getString("campaignNo").orEmpty()
        val expertState by expertViewModel.uiState.collectAsState()
        LaunchedEffect(campaignNo) {
            if (campaignNo.isNotBlank()) expertViewModel.loadCampaignDetail(campaignNo)
        }
        ExpertCampaignDetailScreen(
            campaign = expertState.selectedCampaign,
            isLoading = expertState.isLoadingCampaignDetail,
            errorMessage = expertState.campaignErrorMessage?.asString(),
            onBackClick = navController::popBackStack,
            onRetryClick = { expertViewModel.loadCampaignDetail(campaignNo) }
        )
    }
    composable(
        route = Routes.EXPERT_CASE_DETAIL_WITH_ID,
        arguments = listOf(navArgument("caseId") { type = NavType.StringType })
    ) { backStackEntry ->
        val caseId = backStackEntry.arguments?.getString("caseId").orEmpty()
        val expertState by expertViewModel.uiState.collectAsState()

        LaunchedEffect(caseId) {
            if (caseId.isNotBlank()) expertViewModel.loadCaseDetail(caseId)
        }

        ExpertCaseDetailScreen(
            optimizationCase = expertState.selectedCase,
            isLoading = expertState.isLoadingDetail,
            isSubmitting = expertState.isSubmittingAction,
            errorMessage = expertState.detailErrorMessage?.asString(),
            isNotFound = expertState.isDetailNotFound,
            actionErrorMessage = expertState.actionErrorMessage?.asString(),
            onBackClick = navController::popBackStack,
            onRetryClick = {
                if (expertState.isDetailNotFound) {
                    navController.navigateExpertTopLevel(Routes.EXPERT_CASES)
                } else {
                    expertViewModel.loadCaseDetail(caseId)
                }
            },
            onChangeStatus = expertViewModel::changeCaseStatus,
            onClearActionError = expertViewModel::clearActionError
        )
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
        val profileUser = remember { authState.currentUser }

        AdminProfileScreen(
            userId = profileUser?.id.orEmpty(),
            role = profileUser?.role ?: "ADMIN",
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

private fun NavHostController.navigateExpertTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.EXPERT_HOME)
        launchSingleTop = true
    }
}

private fun NavHostController.navigateAdminTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.ADMIN_HOME)
        launchSingleTop = true
    }
}
