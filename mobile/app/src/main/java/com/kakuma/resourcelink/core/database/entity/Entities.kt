package com.kakuma.resourcelink.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached opportunity row for one list (filter combination), ordered by [position]. */
@Entity(tableName = "opportunities", primaryKeys = ["listKey", "id"], indices = [Index("id")])
data class OpportunityEntity(
    val listKey: String,
    val id: String,
    val position: Int,
    val title: String,
    val category: String,
    val organization: String,
    val summary: String,
    val description: String,
    val requirements: List<String>,
    val location: String,
    val isRemote: Boolean,
    val deadline: String?,
    val applicationUrl: String,
    val isVerified: Boolean,
    val isBookmarked: Boolean,
)

/** Next page to load for a cached list (Paging 3 RemoteMediator). */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val listKey: String,
    val nextPage: Int?,
    val updatedAt: Long,
)

@Entity(tableName = "stories", primaryKeys = ["listKey", "id"], indices = [Index("id")])
data class StoryEntity(
    val listKey: String,
    val id: String,
    val position: Int,
    val title: String,
    val excerpt: String,
    val coverImage: String?,
    val category: String,
    val language: String,
    val status: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String?,
    val authorCountry: String?,
    val readingTime: Int,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val publishedAt: String?,
)

/** Full articles kept for offline reading (every opened article is saved automatically). */
@Entity(tableName = "offline_articles")
data class OfflineArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val excerpt: String,
    val coverImage: String?,
    val category: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String?,
    val authorCountry: String?,
    val readingTime: Int,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val publishedAt: String?,
    val savedAt: Long,
)

enum class DraftSyncState { LOCAL_ONLY, PENDING_UPLOAD, SYNCED }

/** Story drafts are always saved on the device first, so writing works offline. */
@Entity(tableName = "story_drafts")
data class StoryDraftEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val title: String,
    val body: String,
    val category: String,
    val language: String,
    /** Path of a cover image copied into app-private storage (never a raw content:// URI). */
    val coverPath: String?,
    val consentToPublish: Boolean,
    val syncState: DraftSyncState,
    val updatedAt: Long,
)

enum class PendingOperationType { BOOKMARK_OPPORTUNITY, UNBOOKMARK_OPPORTUNITY, BOOKMARK_STORY, UNBOOKMARK_STORY, LIKE_STORY, UNLIKE_STORY }

/** Idempotent actions taken offline, replayed by WorkManager when the network returns. */
@Entity(tableName = "pending_operations", indices = [Index(value = ["type", "targetId"], unique = true)])
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: PendingOperationType,
    val targetId: String,
    val createdAt: Long,
    val attempts: Int = 0,
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val organization: String,
    val description: String,
    val phone: String,
    val email: String,
    val website: String,
    val address: String,
    val openingHours: String,
    val isVerified: Boolean,
)
