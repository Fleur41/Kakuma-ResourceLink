package com.kakuma.resourcelink.core.common

/** Domain-level errors, mapped from the backend's `{"error": {code, message, details}}` envelope. */
sealed interface AppError {
    /** No connection or timeout: the operation may be retried, or queued while offline. */
    data object Network : AppError
    /** Session could not be refreshed; the user must sign in again. */
    data object Unauthorized : AppError
    data object Forbidden : AppError
    data object NotFound : AppError
    data object RateLimited : AppError
    /** The AI assistant (or another backend dependency) is temporarily unavailable. */
    data class Unavailable(val message: String?) : AppError
    data class Validation(val message: String?, val fieldErrors: Map<String, String>) : AppError
    data class Conflict(val message: String?) : AppError
    data class Server(val message: String?) : AppError
    data class Unknown(val message: String?) : AppError
}
