package com.example.offerhub.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offerhub.OfferHubApplication
import com.example.offerhub.navigation.AppNavigation
import com.example.offerhub.viewModel.AuthViewModel
import com.example.offerhub.viewModel.SubscriberViewModel
import com.example.offerhub.viewModel.AdminViewModel
import com.example.offerhub.viewModel.ExpertViewModel
import com.example.offerhub.viewModel.GamificationViewModel
import com.example.offerhub.viewModel.SupervisorViewModel

@Composable
fun OfferHubApp() {
    val application = LocalContext.current.applicationContext as OfferHubApplication
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(application.authRepository)
    )
    val subscriberViewModel: SubscriberViewModel = viewModel(
        factory = SubscriberViewModel.Factory(application.subscriberRepository)
    )
    val adminViewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.Factory(application.adminRepository)
    )
    val expertViewModel: ExpertViewModel = viewModel(
        factory = ExpertViewModel.Factory(application.expertRepository)
    )
    val gamificationViewModel: GamificationViewModel = viewModel(
        factory = GamificationViewModel.Factory(application.gamificationRepository)
    )
    val supervisorViewModel: SupervisorViewModel = viewModel(
        factory = SupervisorViewModel.Factory(application.supervisorRepository)
    )
    AppNavigation(
        authViewModel = authViewModel,
        subscriberViewModel = subscriberViewModel,
        adminViewModel = adminViewModel,
        expertViewModel = expertViewModel,
        gamificationViewModel = gamificationViewModel,
        supervisorViewModel = supervisorViewModel
    )
}
