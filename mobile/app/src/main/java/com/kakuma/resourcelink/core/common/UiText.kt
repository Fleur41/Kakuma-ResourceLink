package com.kakuma.resourcelink.core.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import android.content.Context

/** Text that is resolved in the UI layer so every user-facing string stays localisable. */
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    /** Text that already comes localised from the server (e.g. validation messages). */
    data class Raw(val value: String) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Res -> stringResource(id, *args.toTypedArray())
        is Raw -> value
    }

    fun asString(context: Context): String = when (this) {
        is Res -> context.getString(id, *args.toTypedArray())
        is Raw -> value
    }
}
