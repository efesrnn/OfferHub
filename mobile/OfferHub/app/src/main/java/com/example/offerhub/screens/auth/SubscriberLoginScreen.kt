package com.example.offerhub.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.components.AuthButton
import com.example.offerhub.components.ClickableText
import com.example.offerhub.components.TextFieldComponent
import com.example.offerhub.R

@Composable
fun SubscriberLoginScreen(
    onSendCodeClick:(String)->Unit,
    onRegisterClick:()->Unit,
    isLoading: Boolean = false,
    backendError: String? = null
)
{
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    )
    {
        var gsm by remember {
            mutableStateOf("")
        }
        val gsmIsInvalid=gsm.isBlank()||gsm.length!=10
        var gsmTouched by remember {
            mutableStateOf(false)
        }
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

        ){
            Text(
                text = stringResource(R.string.auth_welcome_back),
                fontSize=27.sp,
                fontWeight=FontWeight.Bold
            )

            Spacer(modifier=Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.auth_phone_login_subtitle),
                fontSize=18.sp,
                //fontfamily,
                fontWeight= FontWeight.Normal
            )
            Spacer(modifier=Modifier.height(20.dp))
            TextFieldComponent(
                value=gsm,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.filter { it.isDigit() }
                    if (digitsOnly.length <= 10) {
                        gsm=digitsOnly
                    }
                },
                label = stringResource(R.string.auth_gsm),
                prefix = stringResource(R.string.auth_phone_prefix),
                keyboardType = KeyboardType.Phone,
                isError = gsmTouched&&gsmIsInvalid,
                errorMessage = when {
                    gsm.isBlank() ->
                        stringResource(R.string.error_gsm_empty)

                    gsm.length < 10 ->
                        stringResource(R.string.error_phone_too_short)

                    else ->
                        stringResource(R.string.error_invalid_gsm)
                },
                onFocusChanged={isFocused->
                    if(!isFocused)
                    {
                        gsmTouched=true
                    }
                }
            )
            Spacer(modifier=Modifier.height(18.dp))
            if (backendError != null) {
                Text(
                    text = backendError,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            AuthButton(
                text = if (isLoading) {
                    stringResource(R.string.auth_sending_code)
                } else {
                    stringResource(R.string.auth_send_code)
                },
                enabled = !isLoading,
                onClick={
                    gsmTouched=true
                    if(!gsmIsInvalid)
                    {
                        onSendCodeClick(gsm)
                    }
                }
            )
            Spacer(modifier=Modifier.height(18.dp))
            ClickableText(
                text = stringResource(R.string.auth_register_link),
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
        onRegisterClick = {},
        isLoading = false
    )
}












