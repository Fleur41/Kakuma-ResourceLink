package com.kakuma.resourcelink.feature.auth.domain

import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import javax.inject.Inject

sealed interface AuthOutcome {
    data class Success(val user: User) : AuthOutcome
    /** Local or server field errors (keys: "email", "password", "full_name", ...). */
    data class InvalidFields(val local: Map<String, FieldError>, val server: Map<String, String> = emptyMap()) : AuthOutcome
    data class Failed(val error: AppError) : AuthOutcome
}

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthOutcome {
        val errors = buildMap {
            CredentialsValidator.email(email)?.let { put("email", it) }
            CredentialsValidator.loginPassword(password)?.let { put("password", it) }
        }
        if (errors.isNotEmpty()) return AuthOutcome.InvalidFields(errors)
        return when (val result = repository.login(email.trim().lowercase(), password)) {
            is AppResult.Success -> AuthOutcome.Success(result.data)
            is AppResult.Failure -> result.error.toOutcome()
        }
    }
}

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(registration: Registration): AuthOutcome {
        val errors = buildMap {
            CredentialsValidator.fullName(registration.fullName)?.let { put("full_name", it) }
            CredentialsValidator.email(registration.email)?.let { put("email", it) }
            CredentialsValidator.newPassword(registration.password)?.let { put("password", it) }
        }
        if (errors.isNotEmpty()) return AuthOutcome.InvalidFields(errors)
        val clean = registration.copy(fullName = registration.fullName.trim(), email = registration.email.trim().lowercase())
        return when (val result = repository.register(clean)) {
            is AppResult.Success -> AuthOutcome.Success(result.data)
            is AppResult.Failure -> result.error.toOutcome()
        }
    }
}

private fun AppError.toOutcome(): AuthOutcome =
    if (this is AppError.Validation && fieldErrors.isNotEmpty()) AuthOutcome.InvalidFields(emptyMap(), fieldErrors)
    else AuthOutcome.Failed(this)
