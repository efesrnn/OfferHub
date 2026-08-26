package com.example.offerhub.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.offerhub.screens.auth.AuthChoiceScreen
import com.example.offerhub.screens.auth.OtpVerificationScreen
import com.example.offerhub.screens.auth.StaffLoginScreen
import com.example.offerhub.screens.auth.SubscriberLoginScreen
import com.example.offerhub.screens.auth.SubscriberRegisterScreen
import com.example.offerhub.screens.subscriber.OfferDetailBottomSheet
import com.example.offerhub.viewModel.AuthViewModel
import com.example.offerhub.viewModel.SubscriberViewModel
import com.example.offerhub.ui.text.asString

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    subscriberViewModel: SubscriberViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val subscriberState by subscriberViewModel.uiState.collectAsState()

    LaunchedEffect(authState.otpReady, authState.pendingPhone) {
        if (authState.otpReady) {
            authState.pendingPhone?.let { phone ->
                navController.navigate(
                    "${Routes.OTP_VERIFICATION}/${Uri.encode(phone)}"
                )
                authViewModel.consumeOtpReady()
            }
        }
    }

    LaunchedEffect(authState.pendingNavigationRole) {
        authState.pendingNavigationRole?.let { role ->
            role.toHomeRoute()?.let { route ->
                navController.navigate(route) {
                    popUpTo(Routes.AUTH_CHOICE) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.consumeAuthenticationNavigation()
            } ?: authViewModel.handleUnsupportedRole()
        }
    }

    NavHost(navController, startDestination = Routes.SPLASH) {
        authGraph(navController, authState, authViewModel)
        subscriberGraph(
            navController,
            subscriberState,
            authViewModel,
            subscriberViewModel
        )
        staffRoleGraphs()
    }
    subscriberState.selectedOffer?.let { offer ->
        OfferDetailBottomSheet(
            offer = offer,
            isSubmitting = subscriberState.isSubmittingAction,
            actionError = subscriberState.actionErrorMessage?.asString(),
            onDismiss = subscriberViewModel::dismissOfferDetail,
            onAcceptClick = subscriberViewModel::acceptOffer,
            onDeclineClick = subscriberViewModel::declineOffer,
            onSubmitRating = subscriberViewModel::rateOffer
        )
    }
}

@Composable
fun SubscriberHomeRoute() {
    TODO("Not yet implemented")
}

private fun String.toHomeRoute(): String? = when (uppercase()) {
    "SUBSCRIBER" -> Routes.SUBSCRIBER_HOME
    "EXPERT" -> Routes.EXPERT_HOME
    "SUPERVISOR" -> Routes.SUPERVISOR_HOME
    "ADMIN" -> Routes.ADMIN_HOME
    else -> null
}
