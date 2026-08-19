package com.example.offerhub.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.offerhub.screens.auth.AuthChoiceScreen
import com.example.offerhub.screens.auth.OtpVerificationScreen
import com.example.offerhub.screens.auth.SplashScreen
import com.example.offerhub.screens.auth.StaffLoginScreen

@Composable
fun AppNavigation()
{
    val navController= rememberNavController()

    NavHost(
        navController=navController,
        startDestination=Routes.SPLASH
    ){
        composable(Routes.SPLASH){
            SplashScreen()
        }
        composable(Routes.AUTH_CHOICE){
            AuthChoiceScreen()
        }
        composable(Routes.STAFF_LOGIN){
            StaffLoginScreen()
        }
        composable(Routes.SUBSCRIBER_LOGIN){
            StaffLoginScreen()
        }
        composable(Routes.SUBSCRIBER_REGISTER){
            StaffLoginScreen()
        }
        composable(Routes.OTP_VERIFICATION){
            OtpVerificationScreen()
        }
    }
}