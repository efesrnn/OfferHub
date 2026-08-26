package com.example.offerhub.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
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
            authState,
            subscriberState,
            authViewModel,
            subscriberViewModel
        )
        staffRoleGraphs()
    }

        composable(Routes.AUTH_CHOICE) {
            AuthChoiceScreen(
                onSubscriberClick = { navController.navigate(Routes.SUBSCRIBER_LOGIN) },
                onStaffClick = { navController.navigate(Routes.STAFF_LOGIN) }
            )
        }

        composable(Routes.STAFF_LOGIN) {
            StaffLoginScreen(
                onLoginClick = authViewModel::staffLogin,
                isLoading = authState.isLoading,
                backendError = authState.errorMessage,
                lockRemainingSeconds = authState.lockRemainingSeconds
            )
        }

        composable(Routes.SUBSCRIBER_LOGIN) {
            SubscriberLoginScreen(
                onSendCodeClick = { phone ->
                    // The API contract has no OTP-request endpoint for existing subscribers yet.
                    navController.navigate("${Routes.OTP_VERIFICATION}/${Uri.encode(phone)}")
                },
                onRegisterClick = { navController.navigate(Routes.SUBSCRIBER_REGISTER) }
            )
        }

        composable(Routes.SUBSCRIBER_REGISTER) {
            SubscriberRegisterScreen(
                onRegisterClick = authViewModel::registerSubscriber,
                onLoginClick = { navController.popBackStack() },
                isLoading = authState.isLoading,
                backendError = authState.errorMessage
            )
        }

        composable(
            route = Routes.OTP_VERIFICATION_WITH_PHONE,
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phoneNumber").orEmpty()
            OtpVerificationScreen(
                phoneNumber = phone,
                onVerifyClick = { otp, useFirebase ->
                    authViewModel.verifyOtp(phone, otp, useFirebase)
                },
                onResendClick = {
                    // Contract gap: wire this when /auth/otp-request is agreed with backend.
                },
                isLoading = authState.isLoading,
                backendError = authState.errorMessage
            )
        }

        composable(Routes.SUBSCRIBER_HOME) { SubscriberHomeRoute() }
        composable(Routes.EXPERT_HOME) { RoleHomePlaceholder("Expert") }
        composable(Routes.SUPERVISOR_HOME) { RoleHomePlaceholder("Supervisor") }
        composable(Routes.ADMIN_HOME) { RoleHomePlaceholder("Admin") }
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

private fun String.toHomeRoute(): String? = when (uppercase()) {
    "SUBSCRIBER" -> Routes.SUBSCRIBER_HOME
    "EXPERT" -> Routes.EXPERT_HOME
    "SUPERVISOR" -> Routes.SUPERVISOR_HOME
    "ADMIN" -> Routes.ADMIN_HOME
    else -> null
}
