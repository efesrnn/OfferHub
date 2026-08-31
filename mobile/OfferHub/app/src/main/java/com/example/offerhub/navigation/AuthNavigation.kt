package com.example.offerhub.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.offerhub.screens.auth.AuthChoiceScreen
import com.example.offerhub.screens.auth.OtpVerificationScreen
import com.example.offerhub.screens.auth.SplashScreen
import com.example.offerhub.screens.auth.StaffLoginScreen
import com.example.offerhub.screens.auth.StaffChangePasswordScreen
import com.example.offerhub.screens.auth.SubscriberLoginScreen
import com.example.offerhub.screens.auth.SubscriberRegisterScreen
import com.example.offerhub.BuildConfig
import com.example.offerhub.viewModel.AuthViewModel
import com.example.offerhub.ui.text.asString

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    composable(Routes.SPLASH) {
        val authState by authViewModel.uiState.collectAsState()
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
        val authState by authViewModel.uiState.collectAsState()
        LaunchedEffect(Unit) {
            authViewModel.clearError()
        }
        StaffLoginScreen(
            onLoginClick = authViewModel::staffLogin,
            isLoading = authState.isLoading,
            backendError = authState.errorMessage?.asString(),
            lockRemainingSeconds = authState.lockRemainingSeconds,
            onMockAdminClick = if (BuildConfig.DEBUG) {
                authViewModel::debugLoginAsAdmin
            } else {
                null
            },
            onMockExpertClick = if (BuildConfig.DEBUG) {
                authViewModel::debugLoginAsExpert
            } else {
                null
            },
            onMockSupervisorClick = if (BuildConfig.DEBUG) {
                authViewModel::debugLoginAsSupervisor
            } else {
                null
            }
        )
    }

    composable(Routes.STAFF_CHANGE_PASSWORD) {
        val authState by authViewModel.uiState.collectAsState()
        val returnToStaffLogin = {
            if (authState.passwordChangeCompleted) {
                authViewModel.finishPasswordChangeFlow()
            } else {
                authViewModel.cancelPasswordChange()
            }
            navController.navigate(Routes.STAFF_LOGIN) {
                popUpTo(Routes.STAFF_LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
        BackHandler(onBack = returnToStaffLogin)
        StaffChangePasswordScreen(
            isLoading = authState.isLoading,
            backendError = authState.errorMessage?.asString(),
            isCompleted = authState.passwordChangeCompleted,
            onChangePassword = authViewModel::changePassword,
            onBackToLogin = returnToStaffLogin
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
        val authState by authViewModel.uiState.collectAsState()
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
        val authState by authViewModel.uiState.collectAsState()
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
