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
import com.example.offerhub.data.model.OfferType
import com.example.offerhub.data.model.RatedOffer
import com.example.offerhub.screens.auth.AuthChoiceScreen
import com.example.offerhub.screens.auth.OtpVerificationScreen
import com.example.offerhub.screens.auth.SplashScreen
import com.example.offerhub.screens.auth.StaffLoginScreen
import com.example.offerhub.screens.auth.SubscriberLoginScreen
import com.example.offerhub.screens.auth.SubscriberRegisterScreen
import com.example.offerhub.screens.subscriber.OffersScreen
import com.example.offerhub.screens.subscriber.SubscriberHomeScreen
import com.example.offerhub.screens.subscriber.SubscriberProfileScreen
import com.example.offerhub.viewModel.AuthViewModel

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val subscriberOffers = listOf(
        Offer(
            offerId = "f1a2",
            campaignNo = "CMP-2026-000123",
            title = "20 GB Internet",
            score = 0.83,
            highlighted = true,
            status = "PENDING",
            type = OfferType.ADD_ON
        ),

        Offer(
            offerId = "f1a3",
            campaignNo = "CMP-2026-000124",
            title = "Advantage Tariff",
            score = 0.76,
            highlighted = true,
            status = "PENDING",
            type = OfferType.TARIFF_UPGRADE
        ),

        Offer(
            offerId = "f1a4",
            campaignNo = "CMP-2026-000125",
            title = "Device Discount",
            score = 0.68,
            highlighted = false,
            status = "PENDING",
            type = OfferType.DEVICE_OFFER
        ),

        Offer(
            offerId = "f1a5",
            campaignNo = "CMP-2026-000126",
            title = "25 GB Internet",
            score = 0.91,
            highlighted = true,
            status = "ACCEPTED",
            type = OfferType.ADD_ON
        ),

        Offer(
            offerId = "f1a6",
            campaignNo = "CMP-2026-000127",
            title = "Loyalty Gift",
            score = 0.72,
            highlighted = false,
            status = "PENDING",
            type = OfferType.LOYALTY
        )
    )
    val subscriberRatedOffers = listOf(
        RatedOffer(
            offer = subscriberOffers.first { offer ->
                offer.offerId == "f1a5"
            },
            rating = 4
        )
    )
    val acceptedOffers =
        subscriberOffers.filter { offer ->
            offer.status == "ACCEPTED"
        }

    val latestAcceptedOffer =
        acceptedOffers.lastOrNull()
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

        composable(Routes.SUBSCRIBER_HOME) {
            SubscriberHomeScreen(
                firstName = "Test",

                recommendedOffers =
                    subscriberOffers.filter { offer ->
                        offer.status == "PENDING"
                    },

                latestAcceptedOffer = latestAcceptedOffer,

                onOfferClick = { offerId ->
                    // ModalBottomSheet sonraki adımda bağlanacak.
                },

                onCategoryClick = { offerType ->
                    navController.navigate(Routes.OFFERS) {
                        launchSingleTop = true
                    }
                },

                onAcceptedOffersClick = {
                    navController.navigate(
                        Routes.ACCEPTED_OFFERS
                    ) {
                        launchSingleTop = true
                    }
                },

                onRatedOffersClick = {
                    navController.navigate(
                        Routes.RATED_OFFERS
                    ) {
                        launchSingleTop = true
                    }
                },

                onHomeClick = {
                    // Zaten Home.
                },

                onOffersClick = {
                    navController.navigate(Routes.OFFERS) {
                        launchSingleTop = true
                    }
                },

                onProfileClick = {
                    navController.navigate(Routes.PROFILE) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.EXPERT_HOME) { RoleHomePlaceholder("Expert") }
        composable(Routes.SUPERVISOR_HOME) { RoleHomePlaceholder("Supervisor") }
        composable(Routes.ADMIN_HOME) { RoleHomePlaceholder("Admin") }
        composable(Routes.OFFERS) {
            OffersScreen(
                offers = subscriberOffers,

                onRetryClick = {
                    // ViewModel bağlanınca tekrar yükleyecek
                },

                onOfferClick = { offerId ->
                    // Offer Detail route sonra bağlanacak
                },

                onHomeClick = {
                    navController.navigate(
                        Routes.SUBSCRIBER_HOME
                    ) {
                        launchSingleTop = true
                        popUpTo(Routes.SUBSCRIBER_HOME)
                    }
                },

                onOffersClick = {
                    // Zaten Offers ekranındayız.
                },
                onAcceptedOffersClick = {
                    navController.navigate(
                        Routes.ACCEPTED_OFFERS
                    ) {
                        launchSingleTop = true
                    }
                },

                onRatedOffersClick = {
                    navController.navigate(
                        Routes.RATED_OFFERS
                    ) {
                        launchSingleTop = true
                    }
                },
                onProfileClick = {
                    navController.navigate(
                        Routes.PROFILE
                    ) {
                        launchSingleTop = true
                    }
                }

            )
        }
        composable(Routes.ACCEPTED_OFFERS) {
            Text(
                text = "My Accepted Offers"
            )
        }
         //temp
        composable(Routes.RATED_OFFERS) {
            Text(
                text = "My Rated Offers"
            )
        }
        composable(Routes.PROFILE) {
            SubscriberProfileScreen(
                firstName = "Test",
                lastName = "Subscriber",
                phone = "+90 555 111 22 33",
                email = "test@offerhub.com",

                acceptedOfferCount =
                    subscriberOffers.count { offer ->
                        offer.status == "ACCEPTED"
                    },

                onRetryClick = {
                    // Profile API bağlandığında
                    // tekrar yükleme yapılacak.
                },

                onLogoutClick = {
                    // Backend logout/token temizleme
                    // bağlandığında ViewModel çağrılacak.

                    navController.navigate(
                        Routes.AUTH_CHOICE
                    ) {
                        popUpTo(
                            Routes.SUBSCRIBER_HOME
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                },

                onHomeClick = {
                    navController.navigate(
                        Routes.SUBSCRIBER_HOME
                    ) {
                        popUpTo(
                            Routes.SUBSCRIBER_HOME
                        ) {
                            inclusive = false
                        }

                        launchSingleTop = true
                    }
                },

                onOffersClick = {
                    navController.navigate(
                        Routes.OFFERS
                    ) {
                        launchSingleTop = true
                    }
                },

                onProfileClick = {
                    // Zaten Profile ekranındayız.
                }
            )
        }
    }
}




@Composable
private fun RoleHomePlaceholder(role: String) {
    Text(text = "$role home")
}
