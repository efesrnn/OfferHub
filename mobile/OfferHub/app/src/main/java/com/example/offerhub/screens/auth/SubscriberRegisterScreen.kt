package com.example.offerhub.screens.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun SubscriberRegisterScreen(
    onRegisterClick: (
        firstName: String,
        lastName: String,
        gsm: String,
        email: String
    ) -> Unit,
    onLoginClick: () -> Unit,
    isLoading: Boolean = false,
    backendError: String? = null
){
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ){
        var firstName by remember{ mutableStateOf("") }
        var lastName by remember {mutableStateOf("")}
        var gsm by remember {mutableStateOf("")}
        var email by remember {mutableStateOf("")}
        var firstNameTouched by remember { mutableStateOf(false) }
        var lastNameTouched by remember { mutableStateOf(false) }
        var gsmTouched by remember { mutableStateOf(false) }
        var emailTouched by remember { mutableStateOf(false) }
        val firstNameIsInvalid=firstName.isBlank()
        val lastNameIsInvalid=lastName.isBlank()
        val gsmIsInvalid=gsm.isBlank()||gsm.length!=10
        val emailIsInvalid=email.isNotBlank()&&
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val formIsValid=
            !firstNameIsInvalid && !lastNameIsInvalid&& !gsmIsInvalid&& !emailIsInvalid
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
                text = stringResource(R.string.auth_create_account),
                fontSize=27.sp,
                fontWeight= FontWeight.Bold
            )
            Spacer(modifier=Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.auth_register_subtitle),
                fontSize=18.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier=Modifier.height(20.dp))
            TextFieldComponent(
                value=firstName,
                onValueChange={firstName=it},
                label = stringResource(R.string.auth_first_name),
                prefix = "",
                keyboardType = KeyboardType.Text,
                isError=firstNameTouched&&firstNameIsInvalid,
                errorMessage = stringResource(R.string.error_first_name_empty),
                onFocusChanged= { isFocused ->
                    if (!isFocused) {
                        firstNameTouched = true
                    }
                }
            )
            Spacer(modifier=Modifier.height(3.dp))
            TextFieldComponent(
                value=lastName,
                onValueChange={lastName=it},
                label = stringResource(R.string.auth_last_name),
                prefix = "",
                keyboardType = KeyboardType.Text,
                isError=lastNameTouched&&lastNameIsInvalid,
                errorMessage = stringResource(R.string.error_last_name_empty),
                onFocusChanged={isFocused->
                    if(!isFocused)
                    {
                        lastNameTouched=true
                    }
                })
            Spacer(modifier=Modifier.height(3.dp))
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
            Spacer(modifier=Modifier.height(3.dp))
            TextFieldComponent(
                value=email,
                onValueChange = {
                    email=it
                },
                label = stringResource(R.string.auth_email),
                prefix="",
                keyboardType = KeyboardType.Text,
                isError = emailTouched && emailIsInvalid,
                errorMessage = stringResource(R.string.error_invalid_email),
                onFocusChanged = {
                        isFocused->
                    if(!isFocused){
                        emailTouched=true
                    }
                },
            )
            Spacer(modifier=Modifier.height(18.dp))
            if (backendError != null) {
                Text(text = backendError, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }
            AuthButton(
                text = if (isLoading) {
                    stringResource(R.string.auth_registering)
                } else {
                    stringResource(R.string.auth_register)
                },
                enabled = !isLoading,
                onClick = {
                    firstNameTouched=true
                    lastNameTouched=true
                    gsmTouched=true
                    emailTouched=true
                    if(formIsValid)
                    {
                        onRegisterClick(
                            firstName,
                            lastName,
                            gsm,
                            email
                        )
                    }
                }
            )
            Spacer(modifier=Modifier.height(18.dp))
            ClickableText(text = stringResource(R.string.auth_login_link),
                onClick=onLoginClick
            )
        }

    }
}

@Preview
@Composable
fun SubscriberRegisterPreview()
{
    SubscriberRegisterScreen(
        onRegisterClick = { _, _, _, _ -> },
        onLoginClick = {}
    )
}
