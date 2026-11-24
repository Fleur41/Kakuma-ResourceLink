package com.kakuma.resourcelink.feature.auth.domain

enum class UserRole(val apiValue: String) {
    MEMBER("member"), ALUMNI("alumni"), MENTOR("mentor"), MODERATOR("moderator"), ADMIN("admin");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.apiValue == value } ?: MEMBER
        /** Roles a person can choose at registration (privileged roles are assigned by admins). */
        val selfAssignable = listOf(MEMBER, ALUMNI, MENTOR)
    }
}

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val preferredLanguage: String,
    val bio: String,
    val location: String,
    val avatarUrl: String?,
    val hasAlumniProfile: Boolean,
) {
    val firstName: String get() = fullName.trim().substringBefore(' ')
    val isModerator: Boolean get() = role == UserRole.MODERATOR || role == UserRole.ADMIN
}
