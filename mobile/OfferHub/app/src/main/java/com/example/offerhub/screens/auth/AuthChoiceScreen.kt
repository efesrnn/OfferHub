package com.example.offerhub.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.components.AuthButton
import com.example.offerhub.components.ClickableText

@Composable
fun AuthChoiceScreen(
    onSubscriberClick:()->Unit,
    onStaffClick:()->Unit
)
{
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(
                    horizontal = 32.dp,
                    vertical = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter= painterResource(R.drawable.logo),
                contentDescription="OfferHub Logo",
                modifier=Modifier.size(150.dp)
            )
            Spacer(modifier= Modifier.height(24.dp))
            Text(
                text = "Welcome to OfferHub",
                fontSize = 27.sp,
                //fontFamily,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "How would you like to continue?",
                fontSize = 18.sp,
                //fontFamily,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(32.dp))

            AuthButton(
                text = "Subscriber",
                onClick = onSubscriberClick
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthButton(
                text = "Staff",
                onClick = onStaffClick
            )
            Spacer(modifier= Modifier.height(18.dp))     //Kaldırılabilir
            ClickableText(
                text="Do you need help ↗",
                onClick = {
                    //
                }
            )
        }
    }
}

@Preview
@Composable
fun AuthChoicePreview()
{
    AuthChoiceScreen(
        onSubscriberClick={},
        onStaffClick={}
    )
}