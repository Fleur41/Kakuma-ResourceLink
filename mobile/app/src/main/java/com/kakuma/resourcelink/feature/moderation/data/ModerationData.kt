package com.kakuma.resourcelink.feature.moderation.data

import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.network.PageDto
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.moderation.domain.ModerationRepository
import com.kakuma.resourcelink.feature.moderation.domain.ReviewProject
import com.kakuma.resourcelink.feature.moderation.domain.ReviewStory
import com.kakuma.resourcelink.feature.projects.data.ProjectDto
import com.kakuma.resourcelink.feature.projects.data.toDomain
import com.kakuma.resourcelink.feature.stories.data.StoryDto
import com.kakuma.resourcelink.feature.stories.data.toDetail
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class RejectRequest(val reason: String)

@JsonClass(generateAdapter = true)
data class NoteRequest(val note: String)

/** Moderator-only endpoints (the backend answers 403 for everyone else). */
interface ModerationApi {
    @GET("moderation/stories/")
    suspend fun stories(@Query("page_size") pageSize: Int = QUEUE_SIZE): PageDto<StoryDto>

    @POST("moderation/stories/{id}/approve/")
    suspend fun approveStory(@Path("id") id: String): StoryDto

    @POST("moderation/stories/{id}/reject/")
    suspend fun rejectStory(@Path("id") id: String, @Body body: RejectRequest): StoryDto

    @GET("moderation/projects/")
    suspend fun projects(@Query("page_size") pageSize: Int = QUEUE_SIZE): PageDto<ProjectDto>

    @POST("moderation/projects/{id}/approve/")
    suspend fun approveProject(@Path("id") id: String, @Body body: NoteRequest): ProjectDto

    @POST("moderation/projects/{id}/reject/")
    suspend fun rejectProject(@Path("id") id: String, @Body body: NoteRequest): ProjectDto

    companion object {
        /** The API's maximum page size; the queue shows the oldest submissions first. */
        const val QUEUE_SIZE = 50
    }
}

@Singleton
class ModerationRepositoryImpl @Inject constructor(private val api: ModerationApi, private val moshi: Moshi) : ModerationRepository {
    override suspend fun pendingStories() = safeApiCall(moshi) { api.stories() }
        .map { page -> page.results.map { ReviewStory(it.toDetail(), it.updatedAt) } }

    override suspend fun pendingProjects() = safeApiCall(moshi) { api.projects() }
        .map { page -> page.results.map { ReviewProject(it.toDomain(), it.createdAt) } }

    override suspend fun approveStory(id: String): AppResult<Unit> = safeApiCall(moshi) { api.approveStory(id) }.map { }

    override suspend fun rejectStory(id: String, reason: String): AppResult<Unit> =
        safeApiCall(moshi) { api.rejectStory(id, RejectRequest(reason.trim())) }.map { }

    override suspend fun decideProject(id: String, approve: Boolean, note: String): AppResult<Unit> = safeApiCall(moshi) {
        val body = NoteRequest(note.trim())
        if (approve) api.approveProject(id, body) else api.rejectProject(id, body)
    }.map { }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ModerationModule {
    @Binds @Singleton abstract fun repository(impl: ModerationRepositoryImpl): ModerationRepository

    companion object {
        @Provides @Singleton fun api(retrofit: Retrofit): ModerationApi = retrofit.create(ModerationApi::class.java)
    }
}
