package com.example.offerhub.screens.auth

import android.R.attr.phoneNumber
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.components.AuthButton
import com.example.offerhub.components.ClickableText
import com.example.offerhub.components.TextFieldComponent

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerifyClick:(String)->Unit,
    onResendClick:()->Unit
)
{
    Surface(
        color=Color.White,
        modifier=Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal=32.dp,vertical=60.dp)
    )
    {
        var otp by remember{mutableStateOf("")}
        Column(
            modifier= Modifier
                .fillMaxSize() ,
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement= Arrangement.Center
        ){
            Text(
                text = "Verify your phone number",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Enter the code sent to $phoneNumber",
                fontSize = 18.sp ,
                fontWeight= FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(18.dp))
            TextFieldComponent(
                value = otp,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        otp = it
                    }
                },
                label = "OTP Code",
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.height(18.dp))
            AuthButton(
                text = "Verify",
                onClick = {
                    onVerifyClick(otp)
                }
            )
            Spacer(modifier = Modifier.height(18.dp))
            ClickableText(text="Resend Code",
                onClick = onResendClick
            )
        }
    }
}

@Preview
@Composable
fun OtpVerificationPreview()
{
    OtpVerificationScreen(
        phoneNumber="+90 *** *** ** **",
        onVerifyClick = {_->},
        onResendClick = {}
    )
}