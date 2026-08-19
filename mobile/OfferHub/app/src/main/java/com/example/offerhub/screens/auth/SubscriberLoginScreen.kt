package com.example.offerhub.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.offerhub.components.gsmTextFieldComponent
import java.util.concurrent.Flow

@Composable
fun SubscriberLoginScreen(
    onSendCodeClick:(String)->Unit,
    onRegisterClick:()->Unit
)
{
    Surface(
        color=Color.White,
        modifier= Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal=32.dp,vertical=60.dp)
    )
    {
        var gsm by remember {
            mutableStateOf("")
        }
        val gsmIsInvalid=gsm.isBlank()
        var gsmTouched by remember {
            mutableStateOf(false)
        }
        Column(
            modifier= Modifier.fillMaxSize(),
            horizontalAlignment= Alignment.CenterHorizontally,
            verticalArrangement= Arrangement.Center
        ){
            Text(
                text="Welcome back",
                fontSize=27.sp,
                fontWeight=FontWeight.Bold
            )

            Spacer(modifier=Modifier.height(18.dp))
            Text(
                text="Log in with your phone number",
                fontSize=18.sp,
                //fontfamily,
                fontWeight= FontWeight.Normal
            )
            Spacer(modifier=Modifier.height(20.dp))
            gsmTextFieldComponent(value=gsm,
                onValueChange = {
                    gsm=it
                })
            Spacer(modifier=Modifier.height(18.dp))
            AuthButton(
                text="Send Code",
                onClick={
                    onSendCodeClick(gsm)
                }
            )
            Spacer(modifier=Modifier.height(18.dp))
            ClickableText(
                text="Don't have an account? Register",
                onClick=onRegisterClick
            )
        }
    }
}

@Preview
@Composable
fun SubscriberLoginScreenPreview()
{
    SubscriberLoginScreen(
        onSendCodeClick = {gsm->},
        onRegisterClick = {}
    )
}












