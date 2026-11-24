package com.kakuma.resourcelink.feature.alumni.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.network.NetworkPagingSource
import com.kakuma.resourcelink.core.network.PageDto
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.alumni.domain.AlumniProfile
import com.kakuma.resourcelink.feature.alumni.domain.AlumniQuery
import com.kakuma.resourcelink.feature.alumni.domain.AlumniRepository
import com.kakuma.resourcelink.feature.alumni.domain.MyAlumniProfile
import com.kakuma.resourcelink.feature.alumni.domain.ProfileVisibility
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class AlumniDto(
    val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "full_name") val fullName: String,
    val avatar: String? = null,
    val country: String = "",
    val city: String? = null,
    val profession: String = "",
    val organization: String = "",
    val skills: List<String> = emptyList(),
    val bio: String = "",
    @Json(name = "is_mentor") val isMentor: Boolean = false,
    @Json(name = "mentorship_areas") val mentorshipAreas: List<String> = emptyList(),
    @Json(name = "willing_to_contribute") val willingToContribute: Boolean = false,
    @Json(name = "contribution_interests") val contributionInterests: List<String> = emptyList(),
    @Json(name = "contact_email") val contactEmail: String? = null,
)

@JsonClass(generateAdapter = true)
data class MyAlumniDto(
    val country: String = "",
    val city: String = "",
    val profession: String = "",
    val organization: String = "",
    val skills: List<String> = emptyList(),
    val bio: String = "",
    @Json(name = "is_mentor") val isMentor: Boolean = false,
    @Json(name = "mentorship_areas") val mentorshipAreas: List<String> = emptyList(),
    @Json(name = "willing_to_contribute") val willingToContribute: Boolean = false,
    @Json(name = "contribution_interests") val contributionInterests: List<String> = emptyList(),
    val visibility: String = "members",
    @Json(name = "show_contact_email") val showContactEmail: Boolean = false,
    @Json(name = "show_city") val showCity: Boolean = true,
    val consent: Boolean = false,
)

interface AlumniApi {
    @GET("alumni/")
    suspend fun list(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
        @Query("search") search: String?,
        @Query("is_mentor") isMentor: Boolean?,
    ): PageDto<AlumniDto>

    @GET("alumni/{id}/")
    suspend fun get(@Path("id") id: String): AlumniDto

    @GET("alumni/profile/")
    suspend fun mine(): Response<MyAlumniDto>

    @POST("alumni/profile/")
    suspend fun create(@Body body: MyAlumniDto): MyAlumniDto

    @PATCH("alumni/profile/")
    suspend fun update(@Body body: MyAlumniDto): MyAlumniDto

    @DELETE("alumni/profile/")
    suspend fun delete(): Response<Unit>
}

fun AlumniDto.toDomain() = AlumniProfile(
    id, userId, fullName, avatar, country, city, profession, organization, skills, bio, isMentor, mentorshipAreas,
    willingToContribute, contributionInterests, contactEmail,
)

fun MyAlumniDto.toDomain() = MyAlumniProfile(
    country, city, profession, organization, skills, bio, isMentor, mentorshipAreas, willingToContribute,
    contributionInterests, ProfileVisibility.from(visibility), showContactEmail, showCity, consent,
)

fun MyAlumniProfile.toDto() = MyAlumniDto(
    country.trim(), city.trim(), profession.trim(), organization.trim(), skills, bio.trim(), isMentor, mentorshipAreas,
    willingToContribute, contributionInterests, visibility.apiValue, showContactEmail, showCity, consent,
)

@Singleton
class AlumniRepositoryImpl @Inject constructor(private val api: AlumniApi, private val moshi: Moshi) : AlumniRepository {
    override fun directory(query: AlumniQuery): Flow<PagingData<AlumniProfile>> = Pager(PagingConfig(pageSize = 20)) {
        NetworkPagingSource(
            fetch = { page, size -> api.list(page, size, query.search.trim().ifBlank { null }, if (query.mentorsOnly) true else null) },
            map = { it.toDomain() },
        )
    }.flow

    override suspend fun get(id: String) = safeApiCall(moshi) { api.get(id) }.map { it.toDomain() }

    override suspend fun myProfile(): AppResult<MyAlumniProfile?> = safeApiCall(moshi) {
        val response = api.mine()
        when {
            response.code() == 404 -> null
            response.isSuccessful -> response.body()?.toDomain()
            else -> throw HttpException(response)
        }
    }

    override suspend fun saveMyProfile(profile: MyAlumniProfile, isNew: Boolean): AppResult<MyAlumniProfile> =
        safeApiCall(moshi) { if (isNew) api.create(profile.toDto()) else api.update(profile.toDto()) }.map { it.toDomain() }

    override suspend fun deleteMyProfile(): AppResult<Unit> = safeApiCall(moshi) {
        val response = api.delete()
        if (!response.isSuccessful && response.code() != 404) throw HttpException(response)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AlumniModule {
    @Binds @Singleton abstract fun repository(impl: AlumniRepositoryImpl): AlumniRepository

    companion object {
        @Provides @Singleton fun api(retrofit: Retrofit): AlumniApi = retrofit.create(AlumniApi::class.java)
    }
}

