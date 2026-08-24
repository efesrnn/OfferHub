package com.example.offerhub.navigation

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.model.OfferType
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
import com.example.offerhub.viewModel.SubscriberViewModel
import com.example.offerhub.screens.subscriber.OfferCategoryScreen
import com.example.offerhub.screens.subscriber.OfferDetailBottomSheet
import com.example.offerhub.R
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    subscriberViewModel: SubscriberViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val subscriberState by subscriberViewModel.uiState.collectAsState()
    val subscriberOffers = subscriberState.offers
    val ratedOffers =
        subscriberOffers.filter { offer ->
            offer.rating != null
        }
    val acceptedOffers =
        subscriberOffers.filter { offer ->
            offer.status == OfferStatus.ACCEPTED
        }
    val openOfferDetail: (String) -> Unit = subscriberViewModel::selectOffer

    val latestAcceptedOffer =
        acceptedOffers.maxByOrNull { offer ->
            offer.acceptedAt.orEmpty()
        }

    LaunchedEffect(authState.otpReady, authState.pendingPhone) {
        if (authState.otpReady) {
            authState.pendingPhone?.let { phone ->
                navController.navigate("${Routes.OTP_VERIFICATION}/${Uri.encode(phone)}")
                authViewModel.consumeOtpReady()
            }
        }
    }

    LaunchedEffect(authState.pendingNavigationRole) {
        authState.pendingNavigationRole?.let { role ->
            val route = when (role.uppercase()) {
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
                authViewModel.consumeAuthenticationNavigation()
            }
        }
    }

    val currentPhone = authState.pendingPhone

    val profilePhone =
        when {
            currentPhone.isNullOrBlank() ->
                stringResource(R.string.profile_not_available)

            currentPhone.startsWith("+") ->
                currentPhone

            else ->
                "+90 $currentPhone"
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
                    authViewModel.setPendingPhone(phone)
                    authViewModel.clearError()

                    navController.navigate(
                        "${Routes.OTP_VERIFICATION}/${Uri.encode(phone)}"
                    )
                },
                onRegisterClick = {
                    authViewModel.clearError()

                    navController.navigate(
                        Routes.SUBSCRIBER_REGISTER
                    )
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
                backendError = authState.errorMessage
            )
        }

        composable(
            route = Routes.OTP_VERIFICATION_WITH_PHONE,
            arguments = listOf(
                navArgument("phoneNumber") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val phone =
                backStackEntry.arguments
                    ?.getString("phoneNumber")
                    .orEmpty()

            LaunchedEffect(phone) {
                authViewModel.clearError()
            }

            OtpVerificationScreen(
                phoneNumber = phone,
                onVerifyClick = { otp, useFirebase ->
                    if (useFirebase) {
                        authViewModel.verifyOtp(phone, otp)
                    } else {
                        navController.navigate(
                            Routes.SUBSCRIBER_HOME
                        ) {
                            popUpTo(
                                Routes.AUTH_CHOICE
                            ) {
                                inclusive = true
                            }
                        }
                    }
                },
                onResendClick = {
                    // Backend OTP resend bağlanacak.
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
                        offer.status == OfferStatus.PENDING
                    },

                latestAcceptedOffer = latestAcceptedOffer,

                onOfferClick = openOfferDetail,

                onCategoryClick = { offerType ->
                    navController.navigate(
                        Routes.offerCategory(offerType.name)
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
        composable(Routes.EXPERT_HOME) {
            RoleHomePlaceholder(stringResource(R.string.role_expert))
        }
        composable(Routes.SUPERVISOR_HOME) {
            RoleHomePlaceholder(stringResource(R.string.role_supervisor))
        }
        composable(Routes.ADMIN_HOME) {
            RoleHomePlaceholder(stringResource(R.string.role_admin))
        }
        composable(Routes.OFFERS) {
            OffersScreen(
                offers = subscriberOffers,
                isLoading = subscriberState.isLoading,
                errorMessage = subscriberState.errorMessage,

                onRetryClick = subscriberViewModel::loadOffers,

                onOfferClick = openOfferDetail,

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
                onSeeAllClick = { offerType ->
                    navController.navigate(
                        Routes.offerCategory(offerType.name)
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
            OfferCategoryScreen(
                title = stringResource(R.string.offers_my_accepted),

                offers = acceptedOffers,

                showAcceptedTag = true,

                emptyMessage =
                    stringResource(R.string.subscriber_no_accepted_offer),

                onBackClick = {
                    navController.popBackStack()
                },

                onOfferClick = openOfferDetail,
            )
        }
        composable(Routes.RATED_OFFERS) {

            val ratingsByOfferId =
                ratedOffers.associate { offer ->
                    offer.offerId to requireNotNull(offer.rating)
                }

            OfferCategoryScreen(
                title = stringResource(R.string.offers_my_rated),
                offers = ratedOffers,
                ratings = ratingsByOfferId,
                emptyMessage =
                    stringResource(R.string.offers_no_rated),

                onBackClick = {
                    navController.popBackStack()
                },

                onOfferClick = openOfferDetail
            )
        }
        composable(Routes.PROFILE) {
            SubscriberProfileScreen(
                firstName = "Test",
                lastName = "Subscriber",
                phone = profilePhone,
                email = "test@offerhub.com",

                onRetryClick = {
                    // Profile API bağlandığında
                    // tekrar yükleme yapılacak.
                },

                onLogoutClick = {
                    authViewModel.logout()

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

        composable(
            route = Routes.OFFER_CATEGORY_WITH_TYPE,

            arguments = listOf(
                navArgument("offerType") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val typeName =
                backStackEntry.arguments
                    ?.getString("offerType")
                    .orEmpty()

            val selectedType =
                runCatching {
                    OfferType.valueOf(typeName)
                }.getOrNull()

            val categoryTitle =
                when (selectedType) {
                    OfferType.ADD_ON ->
                        stringResource(R.string.offers_add_on_packages)

                    OfferType.TARIFF_UPGRADE ->
                        stringResource(R.string.offers_tariff_upgrades)

                    OfferType.DEVICE_OFFER ->
                        stringResource(R.string.offers_device)

                    OfferType.LOYALTY ->
                        stringResource(R.string.offers_loyalty)

                    null ->
                        stringResource(R.string.nav_offers)
                }

            val categoryOffers =
                if (selectedType == null) {
                    emptyList()
                } else {
                    subscriberOffers.filter { offer ->
                        offer.type == selectedType &&
                                offer.status == OfferStatus.PENDING
                    }
                }

            OfferCategoryScreen(
                title = categoryTitle,
                offers = categoryOffers,

                onBackClick = {
                    navController.popBackStack()
                },

                onOfferClick = openOfferDetail
            )
        }
    }
    subscriberState.selectedOffer?.let { offer ->
        OfferDetailBottomSheet(
            offer = offer,
            isSubmitting = subscriberState.isSubmittingAction,
            actionError = subscriberState.errorMessage,

            onDismiss = subscriberViewModel::dismissOfferDetail,
            onAcceptClick = subscriberViewModel::acceptOffer,
            onDeclineClick = subscriberViewModel::declineOffer,
            onSubmitRating = subscriberViewModel::rateOffer
        )
    }
}




@Composable
private fun RoleHomePlaceholder(role: String) {
    Text(text = stringResource(R.string.role_home, role))
}
