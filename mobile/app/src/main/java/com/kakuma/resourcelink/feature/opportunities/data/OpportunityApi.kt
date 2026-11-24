package com.kakuma.resourcelink.feature.opportunities.data

import com.kakuma.resourcelink.core.network.PageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpportunityDto(
    val id: String,
    val title: String,
    val category: String,
    val organization: String,
    val summary: String,
    val description: String,
    val requirements: List<String> = emptyList(),
    val location: String = "",
    @Json(name = "is_remote") val isRemote: Boolean = false,
    val deadline: String? = null,
    @Json(name = "application_url") val applicationUrl: String = "",
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "is_bookmarked") val isBookmarked: Boolean = false,
)

interface OpportunityApi {
    @GET("opportunities/")
    suspend fun list(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("open") open: Boolean? = null,
        @Query("bookmarked") bookmarked: Boolean? = null,
        @Query("ordering") ordering: String? = "deadline",
    ): PageDto<OpportunityDto>

    @GET("opportunities/{id}/")
    suspend fun get(@Path("id") id: String): OpportunityDto

    @POST("opportunities/{id}/bookmark/")
    suspend fun bookmark(@Path("id") id: String): Response<Unit>

    @DELETE("opportunities/{id}/bookmark/")
    suspend fun unbookmark(@Path("id") id: String): Response<Unit>
}
