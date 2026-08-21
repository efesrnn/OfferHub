package com.example.offerhub.screens.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.simulateHotReload
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.components.AuthButton
import com.example.offerhub.components.ClickableText
import com.example.offerhub.components.TextFieldComponent
import com.example.offerhub.ui.theme.OfferHubTheme
import com.example.offerhub.components.PasswordRequirements

fun isPasswordValid(password: String): Boolean {
    return password.length >= 8 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isDigit() } &&
            password.any { !it.isLetterOrDigit() }
}
@Composable
fun StaffLoginScreen(
    onLoginClick: (
        email: String,
        password: String
    ) -> Unit,
    isLoading: Boolean = false,
    backendError: String? = null,
    lockRemainingSeconds: Long = 0
    /*onForgotClick:()->*/
)
{
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    )
    {
        var email by remember{ mutableStateOf("") }
        var password by remember{ mutableStateOf("") }
        var passwordTouched by remember { mutableStateOf(false) }
        val emailIsInvalid=email.isBlank()||!Patterns.EMAIL_ADDRESS.matcher(email).matches()
        var emailTouched by remember { mutableStateOf(false) }
       val passwordIsInvalid= password.isBlank()||!isPasswordValid(password)
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
                text="Welcome back",
                fontSize=27.sp,
                fontWeight=FontWeight.Bold
            )

            Spacer(modifier=Modifier.height(18.dp))
            Text(
                text="Sign in to continue",
                fontSize=18.sp,
                //fontfamily,
                fontWeight= FontWeight.Normal
            )
            Spacer(modifier=Modifier.height(20.dp))
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
            Spacer(modifier=Modifier.height(3.dp))
            TextFieldComponent(
                value=password,
                onValueChange={
                    password=it
                },
                label="Password",
                keyboardType= KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordTouched&&passwordIsInvalid,
                errorMessage = when {
                    password.isBlank() ->
                        "Password cannot be empty"

                    !isPasswordValid(password) ->
                        "ⓘ Password must be at least 8 characters and include an uppercase letter, a number, and a special character."

                    else ->
                        null
                },
                prefix="",
                onFocusChanged = { isFocused ->
                    if (!isFocused) {
                        passwordTouched = true
                    }
                }
            )
            Spacer(modifier=Modifier.height(18.dp))
            if (backendError != null) {
                Text(text = backendError, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                if (lockRemainingSeconds > 0) {
                    val minutes = lockRemainingSeconds / 60
                    val seconds = lockRemainingSeconds % 60
                    Text(text = "Tekrar deneme: %02d:%02d".format(minutes, seconds))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            AuthButton(
                text=if (isLoading) "Signing in..." else "Log In",
                enabled = !isLoading && lockRemainingSeconds == 0L,
                onClick = {
                    emailTouched = true
                    passwordTouched = true
                    if (!emailIsInvalid && !passwordIsInvalid) {
                        onLoginClick(email, password)
                    }
                }
            )
            Spacer(modifier=Modifier.height(18.dp))
            ClickableText  (text="Forgot your password?",
                onClick={
                    /*onForgotClick*/
            } )

        }
    }
}

@Preview
@Composable
fun StaffLoginPreview()
{
    StaffLoginScreen(onLoginClick = {_,_->})

}
