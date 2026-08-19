package com.example.offerhub.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit)
    {
        delay(1500)
        onSplashFinished()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "OfferHub Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "OfferHub",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Preview
@Composable
fun preview()
{
    SplashScreen(onSplashFinished={})
}