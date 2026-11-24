package com.kakuma.resourcelink.core.common

import com.kakuma.resourcelink.R

/** Maps an [AppError] to a short, localised message for Toasts/Snackbars/error states. */
fun AppError.toUiText(): UiText = when (this) {
    AppError.Network -> UiText.Res(R.string.error_network)
    AppError.Unauthorized -> UiText.Res(R.string.error_session_expired)
    AppError.Forbidden -> UiText.Res(R.string.error_forbidden)
    AppError.NotFound -> UiText.Res(R.string.error_not_found)
    AppError.RateLimited -> UiText.Res(R.string.error_rate_limited)
    is AppError.Unavailable -> message?.let(UiText::Raw) ?: UiText.Res(R.string.error_unavailable)
    is AppError.Validation -> message?.let(UiText::Raw) ?: UiText.Res(R.string.error_validation)
    is AppError.Conflict -> message?.let(UiText::Raw) ?: UiText.Res(R.string.error_generic)
    is AppError.Server -> UiText.Res(R.string.error_server)
    is AppError.Unknown -> UiText.Res(R.string.error_generic)
}
