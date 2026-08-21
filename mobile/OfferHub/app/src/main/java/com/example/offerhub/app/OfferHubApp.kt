package com.example.offerhub.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offerhub.OfferHubApplication
import com.example.offerhub.navigation.AppNavigation
import com.example.offerhub.viewModel.AuthViewModel

@Composable
fun OfferHubApp() {
    val application = LocalContext.current.applicationContext as OfferHubApplication
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(application.authRepository)
    )
    AppNavigation(authViewModel)
}
