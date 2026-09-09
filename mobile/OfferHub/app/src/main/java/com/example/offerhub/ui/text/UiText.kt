package com.example.offerhub.ui.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class Resource(
        @param:StringRes val resourceId: Int,
        val formatArgs: List<Any> = emptyList()
    ) : UiText
    data class Dynamic(val value: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(resourceId, *formatArgs.toTypedArray())
    is UiText.Dynamic -> value
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.Resource -> context.getString(resourceId, *formatArgs.toTypedArray())
    is UiText.Dynamic -> value
}
