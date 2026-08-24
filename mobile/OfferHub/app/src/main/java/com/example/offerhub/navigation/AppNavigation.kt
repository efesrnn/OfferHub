package com.example.offerhub.navigation

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.offerhub.data.model.Offer
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
import com.example.offerhub.screens.subscriber.OfferCategoryScreen
import com.example.offerhub.screens.subscriber.OfferDetailBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    var subscriberOffers by remember {
        mutableStateOf(
            listOf(
        Offer(
            offerId = "f1a2",
            campaignNo = "CMP-2026-000123",
            title = "20 GB Internet",
            score = 0.83,
            highlighted = true,
            status = OfferStatus.PENDING,
            type = OfferType.ADD_ON
        ),

        Offer(
            offerId = "f1a3",
            campaignNo = "CMP-2026-000124",
            title = "Advantage Tariff",
            score = 0.76,
            highlighted = true,
            status = OfferStatus.PENDING,
            type = OfferType.TARIFF_UPGRADE
        ),

        Offer(
            offerId = "f1a4",
            campaignNo = "CMP-2026-000125",
            title = "Device Discount",
            score = 0.68,
            highlighted = false,
            status = OfferStatus.PENDING,
            type = OfferType.DEVICE_OFFER
        ),

        Offer(
            offerId = "f1a5",
            campaignNo = "CMP-2026-000126",
            title = "25 GB Internet",
            description =
                "30 gün boyunca geçerli 25 GB internet paketi.",
            discountRate = 20.0,
            validUntil = "2026-09-30T23:59:59Z",
            score = 0.91,
            highlighted = true,
            status = OfferStatus.ACCEPTED,
            type = OfferType.ADD_ON,
            acceptedAt = "2026-08-24T10:30:00Z",
            rating = 4
        ),

        Offer(
            offerId = "f1a6",
            campaignNo = "CMP-2026-000127",
            title = "Loyalty Gift",
            score = 0.72,
            highlighted = false,
            status = OfferStatus.PENDING,
            type = OfferType.LOYALTY
        )
            )
        )
    }
    val ratedOffers =
        subscriberOffers.filter { offer ->
            offer.rating != null
        }
    val acceptedOffers =
        subscriberOffers.filter { offer ->
            offer.status == OfferStatus.ACCEPTED
        }
    var selectedOffer by remember {
        mutableStateOf<Offer?>(null)
    }

    val openOfferDetail: (String) -> Unit = { offerId ->
        selectedOffer =
            subscriberOffers.firstOrNull { offer ->
                offer.offerId == offerId
            }
    }

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

    val currentPhone = authState.pendingPhone

    val profilePhone =
        when {
            currentPhone.isNullOrBlank() ->
                "Not available"

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
        composable(Routes.EXPERT_HOME) { RoleHomePlaceholder("Expert") }
        composable(Routes.SUPERVISOR_HOME) { RoleHomePlaceholder("Supervisor") }
        composable(Routes.ADMIN_HOME) { RoleHomePlaceholder("Admin") }
        composable(Routes.OFFERS) {
            OffersScreen(
                offers = subscriberOffers,

                onRetryClick = {
                    // ViewModel bağlanınca tekrar yükleyecek
                },

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
                title = "My Accepted Offers",

                offers = acceptedOffers,

                showAcceptedTag = true,

                emptyMessage =
                    "You have not accepted an offer yet.",

                onBackClick = {
                    navController.popBackStack()
                },

                onOfferClick = openOfferDetail,
            )
        }
        //temp
        composable(Routes.RATED_OFFERS) {

            val ratingsByOfferId =
                ratedOffers.associate { offer ->
                    offer.offerId to requireNotNull(offer.rating)
                }

            OfferCategoryScreen(
                title = "My Rated Offers",
                offers = ratedOffers,
                ratings = ratingsByOfferId,
                emptyMessage =
                    "You have not rated an offer yet.",

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
                        "Add-on Packages"

                    OfferType.TARIFF_UPGRADE ->
                        "Tariff Upgrades"

                    OfferType.DEVICE_OFFER ->
                        "Device Offers"

                    OfferType.LOYALTY ->
                        "Loyalty Offers"

                    null ->
                        "Offers"
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
    selectedOffer?.let { offer ->
        OfferDetailBottomSheet(
            offer = offer,

            onDismiss = {
                selectedOffer = null
            },

            onAcceptClick = { offerId ->
                val updatedOffer =
                    subscriberOffers
                        .firstOrNull { currentOffer ->
                            currentOffer.offerId == offerId
                        }
                        ?.copy(
                            status = OfferStatus.ACCEPTED,
                            acceptedAt = currentIsoTimestamp()
                        )

                if (updatedOffer != null) {
                    subscriberOffers =
                        subscriberOffers.map { currentOffer ->
                            if (currentOffer.offerId == offerId) {
                                updatedOffer
                            } else {
                                currentOffer
                            }
                        }

                    selectedOffer = updatedOffer
                }
            },

            onDeclineClick = { offerId ->
                subscriberOffers =
                    subscriberOffers.map { currentOffer ->
                        if (currentOffer.offerId == offerId) {
                            currentOffer.copy(
                                status = OfferStatus.DECLINED
                            )
                        } else {
                            currentOffer
                        }
                    }

                selectedOffer = null
            },

            onSubmitRating = { offerId, rating ->
                subscriberOffers =
                    subscriberOffers.map { currentOffer ->
                        if (currentOffer.offerId == offerId) {
                            currentOffer.copy(
                                rating = rating
                            )
                        } else {
                            currentOffer
                        }
                    }

                selectedOffer = null
            }
        )
    }
}




@Composable
private fun RoleHomePlaceholder(role: String) {
    Text(text = "$role home")
}

private fun currentIsoTimestamp(): String {
    return SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())
}