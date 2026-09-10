package com.example.offerhub.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.components.AuthButton
import com.example.offerhub.components.AuthBackButton
import com.example.offerhub.components.PasswordFieldComponent
import com.example.offerhub.components.PasswordRequirements
import com.example.offerhub.components.TextFieldComponent
import com.example.offerhub.data.model.auth.PasswordPolicy
import com.example.offerhub.ui.theme.OfferHubTheme

@Composable
fun ResetPasswordScreen(
    email: String,
    onBackClick: () -> Unit,
    onResetClick: (code: String, newPassword: String) -> Unit,
    isLoading: Boolean = false,
    isCompleted: Boolean = false,
    backendError: String? = null,
    onBackToLoginClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            if (!isCompleted) {
                AuthBackButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isCompleted) {
                    Text(
                        text = stringResource(R.string.auth_password_reset_success_title),
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.auth_password_reset_success_message))
                    Spacer(Modifier.height(24.dp))
                    AuthButton(
                        text = stringResource(R.string.auth_back_to_login),
                        onClick = onBackToLoginClick
                    )
                    return@Column
                }

                var code by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var confirmPassword by remember { mutableStateOf("") }
                var submitted by remember { mutableStateOf(false) }
                val codeIsValid = code.length == 6
                val passwordIsValid = PasswordPolicy.isValid(password)
                val passwordsMatch = password == confirmPassword

                Text(
                    text = stringResource(R.string.auth_reset_password_title_step2),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.auth_reset_code_sent_to, email),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))

                TextFieldComponent(
                    value = code,
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.filter { it.isDigit() }
                        if (digitsOnly.length <= 6) {
                            code = digitsOnly
                        }
                    },
                    label = stringResource(R.string.auth_reset_code),
                    prefix = "",
                    keyboardType = KeyboardType.Number,
                    isError = submitted && !codeIsValid,
                    errorMessage = when {
                        code.isBlank() -> stringResource(R.string.error_reset_code_empty)
                        code.length < 6 -> stringResource(R.string.error_reset_code_length)
                        else -> null
                    }
                )
                Spacer(Modifier.height(10.dp))

                PasswordFieldComponent(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.auth_new_password),
                    isError = submitted && !passwordIsValid,
                    errorMessage = stringResource(R.string.error_password_requirements)
                )
                Spacer(Modifier.height(4.dp))
                PasswordRequirements(password)
                Spacer(Modifier.height(10.dp))

                PasswordFieldComponent(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = stringResource(R.string.auth_confirm_password),
                    isError = submitted && (!passwordsMatch || confirmPassword.isBlank()),
                    errorMessage = stringResource(R.string.error_password_mismatch)
                )

                if (backendError != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(backendError, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(24.dp))
                AuthButton(
                    text = if (isLoading) {
                        stringResource(R.string.auth_resetting_password)
                    } else {
                        stringResource(R.string.auth_reset_password_action)
                    },
                    enabled = !isLoading,
                    onClick = {
                        submitted = true
                        if (codeIsValid && passwordIsValid && passwordsMatch && confirmPassword.isNotBlank()) {
                            onResetClick(code, password)
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordScreenPreview() {
    OfferHubTheme {
        ResetPasswordScreen(
            email = "deneme1@offerhub.com",
            onBackClick = {},
            onResetClick = { _, _ -> }
        )
    }
}
