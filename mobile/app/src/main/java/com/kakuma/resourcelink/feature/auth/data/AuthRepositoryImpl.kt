package com.kakuma.resourcelink.feature.auth.data

import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.common.onSuccess
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.di.IoDispatcher
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.core.security.AuthTokens
import com.kakuma.resourcelink.core.security.TokenStore
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.auth.domain.Registration
import com.kakuma.resourcelink.feature.auth.domain.User
import com.kakuma.resourcelink.feature.auth.domain.UserRole
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

fun UserDto.toDomain() = User(
    id = id,
    email = email,
    fullName = fullName,
    role = UserRole.from(role),
    preferredLanguage = preferredLanguage,
    bio = profile?.bio.orEmpty(),
    location = profile?.location.orEmpty(),
    avatarUrl = profile?.avatar,
    hasAlumniProfile = hasAlumniProfile,
)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val userStore: SessionUserStore,
    private val database: AppDatabase,
    private val moshi: Moshi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    override val isLoggedIn: StateFlow<Boolean> = tokenStore.isLoggedIn
    override val currentUser: Flow<User?> = userStore.user.map { it?.toDomain() }

    override suspend fun login(email: String, password: String): AppResult<User> =
        safeApiCall(moshi) { api.login(LoginRequest(email, password)) }.persistSession()

    override suspend fun register(registration: Registration): AppResult<User> = safeApiCall(moshi) {
        api.register(
            RegisterRequest(
                email = registration.email,
                password = registration.password,
                fullName = registration.fullName,
                role = registration.role.apiValue,
                preferredLanguage = registration.language,
            ),
        )
    }.persistSession()

    private suspend fun AppResult<AuthResponseDto>.persistSession(): AppResult<User> {
        onSuccess {
            tokenStore.save(AuthTokens(it.tokens.access, it.tokens.refresh))
            userStore.save(it.user)
        }
        return map { it.user.toDomain() }
    }

    override suspend fun refreshCurrentUser(): AppResult<User> =
        safeApiCall(moshi) { api.me() }.onSuccess { userStore.save(it) }.map { it.toDomain() }

    override suspend fun updateProfile(fullName: String?, bio: String?, location: String?, language: String?): AppResult<User> {
        val profile = if (bio != null || location != null) ProfilePatch(bio, location) else null
        return safeApiCall(moshi) { api.updateMe(UpdateMeRequest(fullName, language, profile)) }
            .onSuccess { userStore.save(it) }
            .map { it.toDomain() }
    }

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): AppResult<User> {
        val ext = if (mimeType == "image/png") "png" else if (mimeType == "image/webp") "webp" else "jpg"
        val part = MultipartBody.Part.createFormData("avatar", "avatar.$ext", bytes.toRequestBody(mimeType.toMediaType()))
        return safeApiCall(moshi) { api.uploadAvatar(part) }.onSuccess { userStore.save(it) }.map { it.toDomain() }
    }

    override suspend fun requestPasswordReset(email: String): AppResult<Unit> =
        safeApiCall(moshi) { api.requestPasswordReset(EmailRequest(email.trim().lowercase())) }

    override suspend fun logout() {
        tokenStore.tokens()?.refresh?.let { refresh -> safeApiCall(moshi) { api.logout(LogoutRequest(refresh)) } }
        tokenStore.clear()
        userStore.clear()
        // Shared phones are common: remove personal caches, drafts and queued actions.
        withContext(io) { database.clearAllTables() }
    }
}
