package com.kakuma.resourcelink.feature.notifications.domain

import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class NotificationType(val apiValue: String) {
    MENTORSHIP_REQUEST("mentorship_request"), MENTORSHIP_UPDATE("mentorship_update"), STORY_PUBLISHED("story_published"),
    STORY_REJECTED("story_rejected"), STORY_COMMENT("story_comment"), PROJECT_CONTRIBUTION("project_contribution"),
    PROJECT_STATUS("project_status"), SYSTEM("system");
    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } ?: SYSTEM }
}

/** Where tapping a notification leads. */
sealed interface NotificationTarget {
    data class Story(val id: String) : NotificationTarget
    data class Project(val id: String) : NotificationTarget
    data class Mentorship(val received: Boolean) : NotificationTarget
    data object None : NotificationTarget
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: String,
    val data: Map<String, String>,
) {
    val target: NotificationTarget get() = when (type) {
        NotificationType.STORY_PUBLISHED, NotificationType.STORY_REJECTED, NotificationType.STORY_COMMENT ->
            data["story_id"]?.let { NotificationTarget.Story(it) } ?: NotificationTarget.None
        NotificationType.PROJECT_CONTRIBUTION, NotificationType.PROJECT_STATUS ->
            data["project_id"]?.let { NotificationTarget.Project(it) } ?: NotificationTarget.None
        NotificationType.MENTORSHIP_REQUEST -> NotificationTarget.Mentorship(received = true)
        NotificationType.MENTORSHIP_UPDATE -> NotificationTarget.Mentorship(received = false)
        NotificationType.SYSTEM -> NotificationTarget.None
    }
}

interface NotificationRepository {
    val unreadCount: StateFlow<Int>
    fun notifications(): Flow<PagingData<AppNotification>>
    suspend fun refreshUnreadCount(): AppResult<Int>
    suspend fun unread(): AppResult<List<AppNotification>>
    suspend fun markRead(id: String): AppResult<Unit>
    suspend fun markAllRead(): AppResult<Unit>
}
