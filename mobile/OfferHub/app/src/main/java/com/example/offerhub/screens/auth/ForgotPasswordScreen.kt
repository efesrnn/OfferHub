package com.example.offerhub.screens.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.components.AuthButton
import com.example.offerhub.components.AuthBackButton
import com.example.offerhub.components.TextFieldComponent
import com.example.offerhub.ui.theme.OfferHubTheme

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onRequestCodeClick: (String) -> Unit,
    isRequestAvailable: Boolean,
    isLoading: Boolean = false,
    backendError: String? = null
) {
    var email by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val emailIsValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            AuthBackButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.TopStart)
            )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.auth_forgot_password_title),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.auth_forgot_password_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            TextFieldComponent(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.auth_email),
                prefix = "",
                keyboardType = KeyboardType.Email,
                isError = submitted && !emailIsValid,
                errorMessage = if (email.isBlank()) {
                    stringResource(R.string.error_email_empty)
                } else {
                    stringResource(R.string.error_invalid_email)
                }
            )
            if (!isRequestAvailable) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.auth_password_recovery_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            backendError?.let {
                Spacer(Modifier.height(12.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            AuthButton(
                text = stringResource(R.string.auth_request_reset_code),
                enabled = isRequestAvailable && !isLoading,
                onClick = {
                    submitted = true
                    if (emailIsValid) onRequestCodeClick(email.trim())
                }
            )
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    OfferHubTheme {
        ForgotPasswordScreen(
            onBackClick = {},
            onRequestCodeClick = {},
            isRequestAvailable = false
        )
    }
}
