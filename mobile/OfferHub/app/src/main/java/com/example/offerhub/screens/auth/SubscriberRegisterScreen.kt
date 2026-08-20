package com.example.offerhub.screens.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun SubscriberRegisterScreen(
    onRegisterClick: (
        firstName: String,
        lastName: String,
        gsm: String,
        email: String
    ) -> Unit,
    onLoginClick: () -> Unit
){
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
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
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 32.dp,
                    vertical = 48.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text(
                text="Create an Account",
                fontSize=27.sp,
                fontWeight= FontWeight.Bold
            )
            Spacer(modifier=Modifier.height(18.dp))
            Text(
                text="Enter your details to continue",
                fontSize=18.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier=Modifier.height(20.dp))
            TextFieldComponent(
                value=firstName,
                onValueChange={firstName=it},
                label="First Name",
                prefix = "",
                keyboardType = KeyboardType.Text,
            isError=firstNameTouched&&firstNameIsInvalid,
            errorMessage="First name cannot be empty",
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
                label="Last Name",
                prefix = "",
                keyboardType = KeyboardType.Text,
            isError=firstNameTouched&&firstNameIsInvalid,
            errorMessage="First name cannot be empty",
            onFocusChanged={isFocused->
                if(!isFocused)
                {
                    firstNameTouched=true
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
                label="GSM",
                prefix="+90 ",
                keyboardType = KeyboardType.Phone,
                isError = gsmTouched&&gsmIsInvalid,
                errorMessage = when {
                    gsm.isBlank() ->
                        "GSM cannot be empty"

                    gsm.length < 10 ->
                        "Phone number is too short"

                    else ->
                        "Please enter a valid GSM number"
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
                label="Email",
                prefix="",
                keyboardType = KeyboardType.Text,
                isError = emailTouched && emailIsInvalid,
                errorMessage = when {
                    email.isBlank() -> "Email cannot be empty"
                    else -> "Please enter a valid email address"
                },
                onFocusChanged = {
                        isFocused->
                    if(!isFocused){
                        emailTouched=true
                    }
                },
            )
            Spacer(modifier=Modifier.height(18.dp))
            AuthButton(
                text="Register",
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
            ClickableText(text="Already have an account? Log in",
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