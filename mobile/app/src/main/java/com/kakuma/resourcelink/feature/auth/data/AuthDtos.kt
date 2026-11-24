package com.kakuma.resourcelink.feature.auth.data

import com.kakuma.resourcelink.core.network.TokenPairDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileDto(val bio: String = "", val location: String = "", val avatar: String? = null)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val email: String,
    @Json(name = "full_name") val fullName: String,
    val role: String,
    @Json(name = "preferred_language") val preferredLanguage: String,
    val profile: ProfileDto? = null,
    @Json(name = "has_alumni_profile") val hasAlumniProfile: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(val user: UserDto, val tokens: TokenPairDto)

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val password: String,
    @Json(name = "full_name") val fullName: String,
    val role: String,
    @Json(name = "preferred_language") val preferredLanguage: String,
)

@JsonClass(generateAdapter = true)
data class LogoutRequest(val refresh: String)

@JsonClass(generateAdapter = true)
data class EmailRequest(val email: String)

@JsonClass(generateAdapter = true)
data class ProfilePatch(val bio: String? = null, val location: String? = null)

/** PATCH body: null fields are omitted by Moshi, so only changed values are sent. */
@JsonClass(generateAdapter = true)
data class UpdateMeRequest(
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "preferred_language") val preferredLanguage: String? = null,
    val profile: ProfilePatch? = null,
)
