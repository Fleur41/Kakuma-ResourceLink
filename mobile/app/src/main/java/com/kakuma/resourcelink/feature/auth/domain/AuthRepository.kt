package com.kakuma.resourcelink.feature.auth.domain

import com.kakuma.resourcelink.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class Registration(
    val fullName: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val language: String,
)

interface AuthRepository {
    val isLoggedIn: StateFlow<Boolean>
    /** Last known user, persisted so the app can greet the user while offline. */
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): AppResult<User>
    suspend fun register(registration: Registration): AppResult<User>
    suspend fun refreshCurrentUser(): AppResult<User>
    suspend fun updateProfile(fullName: String?, bio: String?, location: String?, language: String?): AppResult<User>
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): AppResult<User>
    /** Removes the profile photo; the person's initial is shown instead. */
    suspend fun removeAvatar(): AppResult<User>
    /** Emails a 6-digit reset code (the server answers the same whether or not the account exists). */
    suspend fun requestPasswordReset(email: String): AppResult<Unit>
    /** Checks the emailed code before asking for a new password. Wrong codes count towards the server's limit. */
    suspend fun verifyPasswordReset(email: String, code: String): AppResult<Unit>
    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String): AppResult<Unit>
    /** Ends the session on the server (best effort) and wipes personal data from the device. */
    suspend fun logout()
}
