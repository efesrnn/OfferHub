package com.example.offerhub.navigation

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.offerhub.screens.auth.AuthChoiceScreen
import com.example.offerhub.screens.auth.OtpVerificationScreen
import com.example.offerhub.screens.auth.SplashScreen
import com.example.offerhub.screens.auth.StaffLoginScreen
import com.example.offerhub.screens.auth.SubscriberLoginScreen
import com.example.offerhub.screens.auth.SubscriberRegisterScreen
import com.example.offerhub.viewModel.AuthUiState
import com.example.offerhub.viewModel.AuthViewModel
import com.example.offerhub.ui.text.asString

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authState: AuthUiState,
    authViewModel: AuthViewModel
) {
    composable(Routes.SPLASH) {
        SplashScreen(
            onSplashFinished = {
                if (authState.currentUser == null) {
                    navController.navigate(Routes.AUTH_CHOICE) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
        )
    }

    composable(Routes.AUTH_CHOICE) {
        AuthChoiceScreen(
            onSubscriberClick = {
                navController.navigate(Routes.SUBSCRIBER_LOGIN)
            },
            onStaffClick = {
                navController.navigate(Routes.STAFF_LOGIN)
            }
        )
    }

    composable(Routes.STAFF_LOGIN) {
        StaffLoginScreen(
            onLoginClick = authViewModel::staffLogin,
            isLoading = authState.isLoading,
            backendError = authState.errorMessage?.asString(),
            lockRemainingSeconds = authState.lockRemainingSeconds
        )
    }

    composable(Routes.SUBSCRIBER_LOGIN) {
        SubscriberLoginScreen(
            onSendCodeClick = { phone ->
                authViewModel.setPendingPhone(phone)
                navController.navigate(
                    "${Routes.OTP_VERIFICATION}/${Uri.encode(phone)}"
                )
            },
            onRegisterClick = {
                authViewModel.clearError()
                navController.navigate(Routes.SUBSCRIBER_REGISTER)
            }
        )
    }

    composable(Routes.SUBSCRIBER_REGISTER) {
        SubscriberRegisterScreen(
            onRegisterClick = authViewModel::registerSubscriber,
            onLoginClick = {
                authViewModel.clearError()
                navController.popBackStack()
            },
            isLoading = authState.isLoading,
            backendError = authState.errorMessage?.asString()
        )
    }

    composable(
        route = Routes.OTP_VERIFICATION_WITH_PHONE,
        arguments = listOf(
            navArgument("phoneNumber") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val phone = backStackEntry.arguments
            ?.getString("phoneNumber")
            .orEmpty()

        LaunchedEffect(phone) {
            authViewModel.clearError()
        }

        OtpVerificationScreen(
            phoneNumber = phone,
            onVerifyClick = { otp, useFirebase ->
                authViewModel.verifyOtp(phone, otp, useFirebase)
            },
            onResendClick = {
                // Backend OTP resend endpoint'i sözleşme kesinleşince bağlanacak.
            },
            isLoading = authState.isLoading,
            backendError = authState.errorMessage?.asString()
        )
    }
}
