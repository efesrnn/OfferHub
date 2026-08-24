package com.example.offerhub.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerifyClick:(otp:String,useFirebase: Boolean)->Unit,
    onResendClick:()->Unit,
    isLoading: Boolean = false,
    backendError: String? = null
)
{

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    )
    {
        var otp by remember { mutableStateOf("") }
        var otpTouched by remember { mutableStateOf(false) }
        var useFirebase by remember { mutableStateOf(false) }
        val otpIsInvalid =
            if (useFirebase) {
                otp.isBlank() || otp.length != 4
            } else {
                otp.length != 4 || otp != "1234"
            }
        // TODO:
        // Mock mode uses fixed OTP "1234".
        // Firebase mode only validates OTP format locally.
        // Real Firebase/backend verification will be connected later.
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment=Alignment.CenterVertically
            ) {
                Text(
                    text = if (useFirebase) {
                        stringResource(R.string.auth_firebase_otp)
                    } else {
                        stringResource(R.string.auth_mock_otp)
                    },
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                Switch(
                    checked = useFirebase,
                    onCheckedChange = {
                        useFirebase = it
                        otp = ""
                        otpTouched = false
                    }
                )
            }
            Spacer(modifier=Modifier.height(50.dp))
            Text(
                text = stringResource(R.string.auth_verify_phone),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.auth_otp_sent_to, phoneNumber),
                fontSize = 18.sp ,
                fontWeight= FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(18.dp))
            TextFieldComponent(
                value = otp,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.filter { it.isDigit() }

                    if (digitsOnly.length <= 4) {
                        otp = digitsOnly
                    }
                },
                label = stringResource(R.string.auth_otp_code),
                prefix = "",
                keyboardType = KeyboardType.Number,

                isError = otpTouched && otpIsInvalid,

                errorMessage = when {
                    otp.isBlank() ->
                        stringResource(R.string.error_otp_empty)

                    otp.length < 4 ->
                        stringResource(R.string.error_otp_length)

                    !useFirebase && otp != "1234" ->
                        stringResource(R.string.error_invalid_mock_otp)

                    else ->
                        null
                }
            )
            Spacer(modifier = Modifier.height(18.dp))
            if (backendError != null) {
                Text(text = backendError, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }
            AuthButton(
                text = if (isLoading) {
                    stringResource(R.string.auth_verifying)
                } else {
                    stringResource(R.string.auth_verify)
                },
                enabled = !isLoading,
                onClick = {
                    otpTouched = true

                    if (!otpIsInvalid) {
                        onVerifyClick(otp,useFirebase)
                    }
                }
            )
            Spacer(modifier = Modifier.height(18.dp))
            ClickableText(text = stringResource(R.string.auth_resend_code),
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
        onVerifyClick = {_,_->},
        onResendClick = {}
    )
}
