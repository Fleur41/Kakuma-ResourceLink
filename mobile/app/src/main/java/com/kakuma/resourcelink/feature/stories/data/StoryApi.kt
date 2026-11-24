package com.kakuma.resourcelink.feature.stories.data

import com.kakuma.resourcelink.core.network.PageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class AuthorDto(
    val id: String,
    @Json(name = "full_name") val fullName: String,
    val avatar: String? = null,
    val country: String? = null,
)

@JsonClass(generateAdapter = true)
data class StoryDto(
    val id: String,
    val title: String,
    val excerpt: String = "",
    @Json(name = "cover_image") val coverImage: String? = null,
    val category: String,
    val language: String = "en",
    val status: String,
    val author: AuthorDto,
    @Json(name = "reading_time") val readingTime: Int = 1,
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "comments_count") val commentsCount: Int = 0,
    @Json(name = "is_liked") val isLiked: Boolean = false,
    @Json(name = "is_bookmarked") val isBookmarked: Boolean = false,
    @Json(name = "published_at") val publishedAt: String? = null,
    val body: String? = null,
    @Json(name = "rejection_reason") val rejectionReason: String? = null,
)

@JsonClass(generateAdapter = true)
data class StoryWriteRequest(
    val title: String,
    val body: String,
    val category: String,
    val language: String,
    @Json(name = "consent_to_publish") val consentToPublish: Boolean,
)

@JsonClass(generateAdapter = true)
data class LikeStateDto(val liked: Boolean, @Json(name = "likes_count") val likesCount: Int)

@JsonClass(generateAdapter = true)
data class CommentDto(val id: String, val author: AuthorDto, val body: String, @Json(name = "created_at") val createdAt: String)

@JsonClass(generateAdapter = true)
data class CommentRequest(val body: String)

interface StoryApi {
    @GET("stories/")
    suspend fun list(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
        @Query("category") category: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("bookmarked") bookmarked: Boolean? = null,
        @Query("mine") mine: Boolean? = null,
    ): PageDto<StoryDto>

    @GET("stories/{id}/")
    suspend fun get(@Path("id") id: String): StoryDto

    @POST("stories/")
    suspend fun create(@Body body: StoryWriteRequest): StoryDto

    @PATCH("stories/{id}/")
    suspend fun update(@Path("id") id: String, @Body body: StoryWriteRequest): StoryDto

    @Multipart
    @PATCH("stories/{id}/")
    suspend fun uploadCover(@Path("id") id: String, @Part cover: MultipartBody.Part, @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody> = emptyMap()): StoryDto

    @POST("stories/{id}/publish/")
    suspend fun publish(@Path("id") id: String): StoryDto

    @DELETE("stories/{id}/")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @POST("stories/{id}/like/")
    suspend fun like(@Path("id") id: String): Response<LikeStateDto>

    @DELETE("stories/{id}/like/")
    suspend fun unlike(@Path("id") id: String): Response<LikeStateDto>

    @POST("stories/{id}/bookmark/")
    suspend fun bookmark(@Path("id") id: String): Response<Unit>

    @DELETE("stories/{id}/bookmark/")
    suspend fun unbookmark(@Path("id") id: String): Response<Unit>

    @GET("stories/{id}/comments/")
    suspend fun comments(@Path("id") id: String, @Query("page_size") pageSize: Int = 50): PageDto<CommentDto>

    @POST("stories/{id}/comments/")
    suspend fun addComment(@Path("id") id: String, @Body body: CommentRequest): CommentDto
}
