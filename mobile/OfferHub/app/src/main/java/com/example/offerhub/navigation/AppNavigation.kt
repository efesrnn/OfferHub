package com.example.offerhub.navigation

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.offerhub.data.model.Offer
import com.example.offerhub.data.model.RatedOffer
import com.example.offerhub.screens.auth.AuthChoiceScreen
import com.example.offerhub.screens.auth.OtpVerificationScreen
import com.example.offerhub.screens.auth.SplashScreen
import com.example.offerhub.screens.auth.StaffLoginScreen
import com.example.offerhub.screens.auth.SubscriberLoginScreen
import com.example.offerhub.screens.auth.SubscriberRegisterScreen
import com.example.offerhub.screens.subscriber.SubscriberHomeScreen
import com.example.offerhub.viewModel.AuthViewModel

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authState.otpReady, authState.pendingPhone) {
        if (authState.otpReady) {
            authState.pendingPhone?.let { phone ->
                navController.navigate("${Routes.OTP_VERIFICATION}/${Uri.encode(phone)}")
                authViewModel.consumeOtpReady()
            }
        }
    }

    LaunchedEffect(authState.authenticatedUser) {
        authState.authenticatedUser?.let { user ->
            val route = when (user.role.uppercase()) {
                "SUBSCRIBER" -> Routes.SUBSCRIBER_HOME
                "EXPERT" -> Routes.EXPERT_HOME
                "SUPERVISOR" -> Routes.SUPERVISOR_HOME
                "ADMIN" -> Routes.ADMIN_HOME
                else -> null
            }
            if (route == null) {
                authViewModel.clearError()
            } else {
                navController.navigate(route) {
                    popUpTo(Routes.AUTH_CHOICE) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.consumeAuthentication()
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.AUTH_CHOICE) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
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
                    if (useFirebase) {
                        authViewModel.verifyOtp(phone, otp)
                    } else {
                        // Preserve the development-only fixed OTP path and its switch behavior.
                        navController.navigate(Routes.SUBSCRIBER_HOME) {
                            popUpTo(Routes.AUTH_CHOICE) { inclusive = true }
                        }
                    }
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
    }
}

@Composable
private fun SubscriberHomeRoute() {
    val offers = listOf(
        Offer("f1a2", "CMP-2026-000123", "20 GB Internet", 0.83, true, "PENDING"),
        Offer("f1a3", "CMP-2026-000124", "Social Media Plus", 0.76, false, "PENDING"),
        Offer("f1a4", "CMP-2026-000125", "Weekend Package", 0.68, false, "PENDING")
    )
    val acceptedOffers = listOf(
        Offer("f1a5", "CMP-2026-000126", "25 GB Internet", 0.91, true, "ACCEPTED")
    )
    SubscriberHomeScreen(
        firstName = "Test",
        recommendedOffers = offers,
        acceptedOffers = acceptedOffers,
        ratedOffers = listOf(RatedOffer(acceptedOffers.first(), 4)),
        onOfferClick = {}
    )
}

@Composable
private fun RoleHomePlaceholder(role: String) {
    Text(text = "$role home")
}
