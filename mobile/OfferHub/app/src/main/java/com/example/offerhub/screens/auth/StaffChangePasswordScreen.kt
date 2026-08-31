package com.example.offerhub.screens.auth

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offerhub.R
import com.example.offerhub.components.AuthButton
import com.example.offerhub.components.ClickableText
import com.example.offerhub.components.PasswordRequirements
import com.example.offerhub.components.TextFieldComponent
import com.example.offerhub.data.model.auth.PasswordPolicy
import com.example.offerhub.ui.theme.OfferHubTheme

@Composable
fun StaffChangePasswordScreen(
    isLoading: Boolean,
    backendError: String?,
    isCompleted: Boolean,
    onChangePassword: (String, String) -> Unit,
    onBackToLogin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isCompleted) {
                Text(
                    text = stringResource(R.string.auth_password_changed_title),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.auth_password_changed_message))
                Spacer(Modifier.height(24.dp))
                AuthButton(
                    text = stringResource(R.string.auth_back_to_login),
                    onClick = onBackToLogin
                )
                return@Column
            }

            var password by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var submitted by remember { mutableStateOf(false) }
            val passwordIsValid = PasswordPolicy.isValid(password)
            val passwordsMatch = password == confirmPassword

            Text(
                text = stringResource(R.string.auth_create_new_password),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.auth_temporary_password_message))
            Spacer(Modifier.height(24.dp))

            TextFieldComponent(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.auth_new_password),
                prefix = "",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                isError = submitted && !passwordIsValid,
                errorMessage = stringResource(R.string.error_password_requirements)
            )
            Spacer(Modifier.height(4.dp))
            PasswordRequirements(password)
            Spacer(Modifier.height(10.dp))

            TextFieldComponent(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = stringResource(R.string.auth_confirm_password),
                prefix = "",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
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
                    stringResource(R.string.auth_changing_password)
                } else {
                    stringResource(R.string.auth_change_password)
                },
                enabled = !isLoading,
                onClick = {
                    submitted = true
                    if (passwordIsValid && passwordsMatch && confirmPassword.isNotBlank()) {
                        onChangePassword(password, confirmPassword)
                    }
                }
            )
            Spacer(Modifier.height(18.dp))
            ClickableText(
                text = stringResource(R.string.auth_back_to_login),
                onClick = onBackToLogin
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StaffChangePasswordScreenPreview() {
    OfferHubTheme {
        StaffChangePasswordScreen(
            isLoading = false,
            backendError = null,
            isCompleted = false,
            onChangePassword = { _, _ -> },
            onBackToLogin = {}
        )
    }
}
