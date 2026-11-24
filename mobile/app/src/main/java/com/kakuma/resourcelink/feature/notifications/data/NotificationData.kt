package com.kakuma.resourcelink.feature.notifications.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.common.onSuccess
import com.kakuma.resourcelink.core.network.NetworkPagingSource
import com.kakuma.resourcelink.core.network.PageDto
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.notifications.domain.AppNotification
import com.kakuma.resourcelink.feature.notifications.domain.NotificationRepository
import com.kakuma.resourcelink.feature.notifications.domain.NotificationType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String = "",
    val data: Map<String, Any?> = emptyMap(),
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class CountDto(val count: Int)

interface NotificationApi {
    @GET("notifications/")
    suspend fun list(@Query("page") page: Int, @Query("page_size") size: Int, @Query("unread") unread: Boolean? = null): PageDto<NotificationDto>

    @GET("notifications/unread-count/")
    suspend fun unreadCount(): CountDto

    @POST("notifications/{id}/read/")
    suspend fun markRead(@Path("id") id: String): retrofit2.Response<Unit>

    @POST("notifications/read-all/")
    suspend fun markAllRead(): retrofit2.Response<Unit>
}

fun NotificationDto.toDomain() = AppNotification(
    id, NotificationType.from(type), title, body, isRead, createdAt,
    data.mapNotNull { (k, v) -> v?.toString()?.let { k to it } }.toMap(),
)

@Singleton
class NotificationRepositoryImpl @Inject constructor(private val api: NotificationApi, private val moshi: Moshi) : NotificationRepository {
    private val _unread = MutableStateFlow(0)
    override val unreadCount: StateFlow<Int> = _unread.asStateFlow()

    override fun notifications(): Flow<PagingData<AppNotification>> = Pager(PagingConfig(pageSize = 20)) {
        NetworkPagingSource({ page, size -> api.list(page, size) }, { it.toDomain() })
    }.flow

    override suspend fun refreshUnreadCount() = safeApiCall(moshi) { api.unreadCount().count }.onSuccess { _unread.value = it }

    override suspend fun unread() = safeApiCall(moshi) { api.list(1, 20, unread = true) }.map { p -> p.results.map { it.toDomain() } }

    override suspend fun markRead(id: String): AppResult<Unit> = safeApiCall(moshi) { api.markRead(id); Unit }
        .onSuccess { _unread.value = (_unread.value - 1).coerceAtLeast(0) }

    override suspend fun markAllRead(): AppResult<Unit> = safeApiCall(moshi) { api.markAllRead(); Unit }.onSuccess { _unread.value = 0 }
}
