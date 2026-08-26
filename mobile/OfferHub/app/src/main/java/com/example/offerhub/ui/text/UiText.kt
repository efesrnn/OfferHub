package com.example.offerhub.ui.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class Resource(@param:StringRes val resourceId: Int) : UiText
    data class Dynamic(val value: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(resourceId)
    is UiText.Dynamic -> value
}
