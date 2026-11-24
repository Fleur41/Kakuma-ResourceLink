package com.kakuma.resourcelink.feature.auth.domain

/** Field-level checks for instant inline feedback. The server remains the source of truth. */
enum class FieldError { REQUIRED, INVALID_EMAIL, PASSWORD_TOO_SHORT, NAME_TOO_SHORT }

object CredentialsValidator {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    const val MIN_PASSWORD = 8

    fun email(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        !EMAIL.matches(value.trim()) -> FieldError.INVALID_EMAIL
        else -> null
    }

    fun loginPassword(value: String): FieldError? = if (value.isEmpty()) FieldError.REQUIRED else null

    fun newPassword(value: String): FieldError? = when {
        value.isEmpty() -> FieldError.REQUIRED
        value.length < MIN_PASSWORD -> FieldError.PASSWORD_TOO_SHORT
        else -> null
    }

    fun fullName(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        value.trim().length < 2 -> FieldError.NAME_TOO_SHORT
        else -> null
    }
}
