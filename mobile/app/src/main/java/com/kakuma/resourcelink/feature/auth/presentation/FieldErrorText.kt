package com.kakuma.resourcelink.feature.auth.presentation

import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.feature.auth.domain.CredentialsValidator
import com.kakuma.resourcelink.feature.auth.domain.FieldError

fun FieldError.toUiText(): UiText = when (this) {
    FieldError.REQUIRED -> UiText.Res(R.string.validation_required)
    FieldError.INVALID_EMAIL -> UiText.Res(R.string.validation_email)
    FieldError.PASSWORD_TOO_SHORT -> UiText.Res(R.string.validation_password_length, listOf(CredentialsValidator.MIN_PASSWORD))
    FieldError.NAME_TOO_SHORT -> UiText.Res(R.string.validation_name_length)
}

/** Merge local (typed) and server (already localised) errors into displayable text per field. */
fun fieldErrors(local: Map<String, FieldError>, server: Map<String, String>): Map<String, UiText> =
    local.mapValues { it.value.toUiText() } + server.mapValues { UiText.Raw(it.value) }
