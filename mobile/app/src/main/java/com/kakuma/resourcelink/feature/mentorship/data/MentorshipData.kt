package com.kakuma.resourcelink.feature.mentorship.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.network.NetworkPagingSource
import com.kakuma.resourcelink.core.network.PageDto
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.mentorship.domain.HelpArea
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipBox
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipRepository
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipRequest
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipStatus
import com.kakuma.resourcelink.feature.stories.data.AuthorDto
import com.kakuma.resourcelink.feature.stories.data.toDomain
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class MentorshipDto(
    val id: String,
    val mentee: AuthorDto,
    val mentor: AuthorDto,
    val goal: String,
    @Json(name = "help_areas") val helpAreas: List<String> = emptyList(),
    val message: String,
    val status: String,
    @Json(name = "response_message") val responseMessage: String? = null,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class CreateMentorshipRequest(
    @Json(name = "mentor_id") val mentorId: String,
    val goal: String,
    @Json(name = "help_areas") val helpAreas: List<String>,
    val message: String,
)

@JsonClass(generateAdapter = true)
data class UpdateMentorshipRequest(
    val status: String,
    @Json(name = "response_message") val responseMessage: String? = null,
)

interface MentorshipApi {
    @GET("mentorship/requests/")
    suspend fun list(@Query("box") box: String, @Query("page") page: Int, @Query("page_size") pageSize: Int): PageDto<MentorshipDto>

    @POST("mentorship/requests/")
    suspend fun create(@Body body: CreateMentorshipRequest): MentorshipDto

    @PATCH("mentorship/requests/{id}/")
    suspend fun update(@Path("id") id: String, @Body body: UpdateMentorshipRequest): MentorshipDto
}

fun MentorshipDto.toDomain() = MentorshipRequest(
    id, mentee.toDomain(), mentor.toDomain(), goal, helpAreas.map(HelpArea::from), message,
    MentorshipStatus.from(status), responseMessage.orEmpty(), createdAt,
)

@Singleton
class MentorshipRepositoryImpl @Inject constructor(private val api: MentorshipApi, private val moshi: Moshi) : MentorshipRepository {
    override fun requests(box: MentorshipBox): Flow<PagingData<MentorshipRequest>> = Pager(PagingConfig(pageSize = 20)) {
        NetworkPagingSource({ page, size -> api.list(box.apiValue, page, size) }, { it.toDomain() })
    }.flow

    override suspend fun send(mentorProfileId: String, goal: String, helpAreas: Set<HelpArea>, message: String): AppResult<MentorshipRequest> =
        safeApiCall(moshi) {
            api.create(CreateMentorshipRequest(mentorProfileId, goal.trim(), helpAreas.map { it.apiValue }, message.trim()))
        }.map { it.toDomain() }

    override suspend fun updateStatus(id: String, status: MentorshipStatus, response: String?) =
        safeApiCall(moshi) { api.update(id, UpdateMentorshipRequest(status.apiValue, response?.trim()?.ifBlank { null })) }.map { it.toDomain() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MentorshipModule {
    @Binds @Singleton abstract fun repository(impl: MentorshipRepositoryImpl): MentorshipRepository

    companion object {
        @Provides @Singleton fun api(retrofit: Retrofit): MentorshipApi = retrofit.create(MentorshipApi::class.java)
    }
}
