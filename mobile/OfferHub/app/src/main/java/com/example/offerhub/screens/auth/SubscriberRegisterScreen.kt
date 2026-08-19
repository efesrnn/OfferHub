package com.example.offerhub.screens.auth

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
import com.example.offerhub.components.EmailTextFieldComponent
import com.example.offerhub.components.TextFieldComponent
import com.example.offerhub.components.gsmTextFieldComponent

@Composable
fun SubscriberRegisterScreen(
    /*onRegisterClick: (
        firstName: String,
        lastName: String,
        gsm: String,
        email: String
    ) -> Unit,
    onLoginClick: () -> Unit*/
){
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        var firstName by remember{ mutableStateOf("") }
        var lastName by remember {mutableStateOf("")}
        var gsm by remember {mutableStateOf("")}
        var email by remember {mutableStateOf("")}
        var emailTouched by remember { mutableStateOf(false) }
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
                keyboardType = KeyboardType.Text)
            Spacer(modifier=Modifier.height(3.dp))
            TextFieldComponent(
                value=lastName,
                onValueChange={lastName=it},
                label="Last Name",
                keyboardType = KeyboardType.Text)
            Spacer(modifier=Modifier.height(3.dp))
            gsmTextFieldComponent(value=gsm,
                onValueChange = {
                    gsm=it
                })
            Spacer(modifier=Modifier.height(3.dp))
            EmailTextFieldComponent(
                value=email,
                onValueChange = {
                    email=it
                },
                emailTouched=emailTouched,
                onFocusChanged = {
                        isFocused->
                    if(!isFocused){
                        emailTouched=true
                    }
                },
                required=false
            )
            Spacer(modifier=Modifier.height(18.dp))
            AuthButton(
                text="Register",
                onClick = {

                }
            )
            Spacer(modifier=Modifier.height(18.dp))
            ClickableText(text="Already have an account? Log in",
                onClick={

                })


        }


    }
}

@Preview
@Composable
fun SubscriberRegisterPreview()
{
    SubscriberRegisterScreen()
}