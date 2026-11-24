package com.kakuma.resourcelink.feature.alumni.domain

import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import kotlinx.coroutines.flow.Flow

data class AlumniProfile(
    val id: String,
    val userId: String,
    val fullName: String,
    val avatarUrl: String?,
    val country: String,
    val city: String?,
    val profession: String,
    val organization: String,
    val skills: List<String>,
    val bio: String,
    val isMentor: Boolean,
    val mentorshipAreas: List<String>,
    val willingToContribute: Boolean,
    val contributionInterests: List<String>,
    val contactEmail: String?,
)

enum class ProfileVisibility(val apiValue: String) {
    PUBLIC("public"), MEMBERS("members"), PRIVATE("private");
    companion object { fun from(v: String?) = entries.firstOrNull { it.apiValue == v } ?: MEMBERS }
}

/** The signed-in user's own alumni profile, including privacy settings. */
data class MyAlumniProfile(
    val country: String = "",
    val city: String = "",
    val profession: String = "",
    val organization: String = "",
    val skills: List<String> = emptyList(),
    val bio: String = "",
    val isMentor: Boolean = false,
    val mentorshipAreas: List<String> = emptyList(),
    val willingToContribute: Boolean = false,
    val contributionInterests: List<String> = emptyList(),
    val visibility: ProfileVisibility = ProfileVisibility.MEMBERS,
    val showContactEmail: Boolean = false,
    val showCity: Boolean = true,
    val consent: Boolean = false,
)

data class AlumniQuery(val search: String = "", val mentorsOnly: Boolean = false)

interface AlumniRepository {
    fun directory(query: AlumniQuery): Flow<PagingData<AlumniProfile>>
    suspend fun get(id: String): AppResult<AlumniProfile>
    /** Success(null) when the user has not created a profile yet. */
    suspend fun myProfile(): AppResult<MyAlumniProfile?>
    suspend fun saveMyProfile(profile: MyAlumniProfile, isNew: Boolean): AppResult<MyAlumniProfile>
    suspend fun deleteMyProfile(): AppResult<Unit>
}

/** "React, Python ,  teaching" → ["React", "Python", "teaching"] (deduplicated, max 20, max 50 chars). */
fun parseTags(input: String): List<String> =
    input.split(',').map { it.trim().take(50) }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.take(20)
