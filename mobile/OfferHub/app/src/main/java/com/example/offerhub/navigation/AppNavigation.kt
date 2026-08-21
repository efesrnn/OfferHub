package com.example.offerhub.navigation

import android.R.attr.phoneNumber
import android.R.attr.type
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.offerhub.screens.auth.AuthChoiceScreen
import com.example.offerhub.screens.auth.OtpVerificationScreen
import com.example.offerhub.screens.auth.SplashScreen
import com.example.offerhub.screens.auth.StaffLoginScreen
import com.example.offerhub.screens.auth.SubscriberLoginScreen
import com.example.offerhub.screens.auth.SubscriberRegisterScreen
import com.example.offerhub.data.model.Campaign
import com.example.offerhub.screens.subscriber.SubscriberHomeScreen
@Composable
fun AppNavigation()
{
    val navController= rememberNavController()

    NavHost(
        navController=navController,
        startDestination=Routes.SPLASH
    ){
        composable(Routes.SUBSCRIBER_HOME) {

            val mockCampaigns = listOf(
                Campaign(
                    campaignId = "1",
                    title = "20 GB Internet",
                    description = "Personalized offer for you.",
                    price = 249.90
                ),
                Campaign(
                    campaignId = "2",
                    title = "Social Media Plus",
                    description = "More data for your favorite apps.",
                    price = 199.90
                ),
                Campaign(
                    campaignId = "3",
                    title = "Weekend Package",
                    description = "Extra internet for weekends.",
                    price = 129.90
                )
            )

            SubscriberHomeScreen(
                firstName = "Test",
                campaigns = mockCampaigns,
                onCampaignClick = { campaignId ->

                },
                onAcceptedCampaignsClick={
                    navController.navigate(Routes.ACCEPTED_CAMPAIGNS)
                }
            )
        }
        composable(Routes.SPLASH){
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.AUTH_CHOICE){
                        popUpTo(Routes.SPLASH){
                            inclusive=true
                        }
                    }
                }
            )
        }
        composable(Routes.AUTH_CHOICE){
            AuthChoiceScreen(
                onSubscriberClick = {
                    navController.navigate(
                        Routes.SUBSCRIBER_LOGIN
                    )
                },

                onStaffClick = {
                    navController.navigate(
                        Routes.STAFF_LOGIN
                    )
                }
            )
        }
        composable(Routes.STAFF_LOGIN){
            StaffLoginScreen(onLoginClick = { email, password ->
                // Daha sonra ViewModel:
                //
                // viewModel.login(email, password)
                //
                // Backend'den role geldikten sonra:
                //
                // EXPERT      -> Expert screen
                // SUPERVISOR  -> Supervisor screen
                // ADMIN       -> Admin screen
            })
        }
        composable(Routes.SUBSCRIBER_LOGIN){
            SubscriberLoginScreen(
                onSendCodeClick = { gsm ->

                    val encodedPhone =
                        Uri.encode(gsm)

                    navController.navigate(
                        "${Routes.OTP_VERIFICATION}/$encodedPhone"
                    )
                },

                onRegisterClick = {
                    navController.navigate(
                        Routes.SUBSCRIBER_REGISTER
                    )
                }
            )
        }
        composable(Routes.SUBSCRIBER_REGISTER){
            SubscriberRegisterScreen(
                onRegisterClick = {
                        firstName,
                        lastName,
                        gsm,
                        email ->

                    // Daha sonra burada register API / ViewModel işlemi yapılacak.

                    val encodedPhone =
                        Uri.encode(gsm)

                    navController.navigate(
                        "${Routes.OTP_VERIFICATION}/$encodedPhone"
                    )
                },

                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.OTP_VERIFICATION_WITH_PHONE,
            arguments = listOf(
                navArgument("phoneNumber") {
                    type = NavType.StringType
                }
            )
        )
        {
            backStackEntry ->
                val phoneNumber =
                    backStackEntry.arguments
                        ?.getString("phoneNumber")
                        ?: ""

            OtpVerificationScreen(
                phoneNumber = phoneNumber,

                onVerifyClick = { otp, useFirebase ->

                    if (useFirebase) {
                        // TODO: Firebase/backend OTP verification
                    } else {
                        // Mock OTP
                        navController.navigate(Routes.SUBSCRIBER_HOME)
                    }
                },
                onResendClick = {
                    // TODO: resend OTP
                }
            )
        }
    }
}