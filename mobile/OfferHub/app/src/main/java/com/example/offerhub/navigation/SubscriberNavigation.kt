package com.example.offerhub.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.offerhub.R
import com.example.offerhub.data.model.OfferStatus
import com.example.offerhub.data.model.OfferType
import com.example.offerhub.screens.subscriber.OfferCategoryScreen
import com.example.offerhub.screens.subscriber.OffersScreen
import com.example.offerhub.screens.subscriber.SubscriberHomeScreen
import com.example.offerhub.screens.subscriber.SubscriberProfileScreen
import com.example.offerhub.viewModel.AuthViewModel
import com.example.offerhub.viewModel.SubscriberUiState
import com.example.offerhub.viewModel.SubscriberViewModel
import com.example.offerhub.ui.text.asString

fun NavGraphBuilder.subscriberGraph(
    navController: NavHostController,
    subscriberState: SubscriberUiState,
    authViewModel: AuthViewModel,
    subscriberViewModel: SubscriberViewModel
) {
    val offers = subscriberState.offers
    val acceptedOffers = offers.filter { it.status == OfferStatus.ACCEPTED }
    val ratedOffers = offers.filter { it.rating != null }
    val latestAcceptedOffer = acceptedOffers.maxByOrNull {
        it.acceptedAt.orEmpty()
    }
    val openOfferDetail: (String) -> Unit = subscriberViewModel::selectOffer

    composable(Routes.SUBSCRIBER_HOME) {
        SubscriberHomeScreen(
            firstName = "Test",
            recommendedOffers = offers.filter {
                it.status == OfferStatus.PENDING
            },
            latestAcceptedOffer = latestAcceptedOffer,
            onOfferClick = openOfferDetail,
            onCategoryClick = { type ->
                navController.navigate(Routes.offerCategory(type.name)) {
                    launchSingleTop = true
                }
            },
            onHomeClick = {},
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

    composable(Routes.OFFERS) {
        OffersScreen(
            offers = offers,
            isLoading = subscriberState.isLoading,
            errorMessage = subscriberState.loadErrorMessage?.asString(),
            onRetryClick = subscriberViewModel::loadOffers,
            onOfferClick = openOfferDetail,
            onHomeClick = {
                navController.navigate(Routes.SUBSCRIBER_HOME) {
                    launchSingleTop = true
                    popUpTo(Routes.SUBSCRIBER_HOME)
                }
            },
            onOffersClick = {},
            onAcceptedOffersClick = {
                navController.navigate(Routes.ACCEPTED_OFFERS) {
                    launchSingleTop = true
                }
            },
            onRatedOffersClick = {
                navController.navigate(Routes.RATED_OFFERS) {
                    launchSingleTop = true
                }
            },
            onSeeAllClick = { type ->
                navController.navigate(Routes.offerCategory(type.name)) {
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

    composable(Routes.ACCEPTED_OFFERS) {
        OfferCategoryScreen(
            title = stringResource(R.string.offers_my_accepted),
            offers = acceptedOffers,
            showAcceptedTag = true,
            emptyMessage = stringResource(R.string.subscriber_no_accepted_offer),
            onBackClick = navController::popBackStack,
            onOfferClick = openOfferDetail
        )
    }

    composable(Routes.RATED_OFFERS) {
        OfferCategoryScreen(
            title = stringResource(R.string.offers_my_rated),
            offers = ratedOffers,
            ratings = ratedOffers.associate {
                it.offerId to requireNotNull(it.rating)
            },
            emptyMessage = stringResource(R.string.offers_no_rated),
            onBackClick = navController::popBackStack,
            onOfferClick = openOfferDetail
        )
    }

    composable(Routes.PROFILE) {
        val authState by authViewModel.uiState.collectAsState()
        val profilePhone = (authState.currentUser?.phone ?: authState.pendingPhone)
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("+")) it else "+90 $it" }
            ?: stringResource(R.string.profile_not_available)

        SubscriberProfileScreen(
            firstName = "Test",
            lastName = "Subscriber",
            phone = profilePhone,
            email = "test@offerhub.com",
            onRetryClick = {},
            onLogoutClick = {
                authViewModel.logout()
                navController.navigate(Routes.AUTH_CHOICE) {
                    popUpTo(Routes.SUBSCRIBER_HOME) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onHomeClick = {
                navController.navigate(Routes.SUBSCRIBER_HOME) {
                    popUpTo(Routes.SUBSCRIBER_HOME) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onOffersClick = {
                navController.navigate(Routes.OFFERS) {
                    launchSingleTop = true
                }
            },
            onProfileClick = {}
        )
    }

    composable(
        route = Routes.OFFER_CATEGORY_WITH_TYPE,
        arguments = listOf(
            navArgument("offerType") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val selectedType = backStackEntry.arguments
            ?.getString("offerType")
            ?.let { runCatching { OfferType.valueOf(it) }.getOrNull() }

        val title = when (selectedType) {
            OfferType.ADD_ON -> stringResource(R.string.offers_add_on_packages)
            OfferType.TARIFF_UPGRADE -> stringResource(R.string.offers_tariff_upgrades)
            OfferType.DEVICE_OFFER -> stringResource(R.string.offers_device)
            OfferType.LOYALTY -> stringResource(R.string.offers_loyalty)
            null -> stringResource(R.string.nav_offers)
        }
        val categoryOffers = offers.filter {
            it.type == selectedType && it.status == OfferStatus.PENDING
        }

        OfferCategoryScreen(
            title = title,
            offers = categoryOffers,
            onBackClick = navController::popBackStack,
            onOfferClick = openOfferDetail
        )
    }
}
