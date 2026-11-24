package com.kakuma.resourcelink.feature.stories.domain

import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.feature.opportunities.domain.SyncOutcome
import kotlinx.coroutines.flow.Flow

enum class StoryCategory(val apiValue: String) {
    EDUCATION("education"), CAREER("career"), RESETTLEMENT("resettlement"), ENTREPRENEURSHIP("entrepreneurship"),
    INSPIRATION("inspiration"), ADVICE("advice"), COMMUNITY("community");

    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } ?: INSPIRATION }
}

enum class StoryStatus(val apiValue: String) {
    DRAFT("draft"), PENDING_REVIEW("pending_review"), PUBLISHED("published"), REJECTED("rejected"), ARCHIVED("archived");

    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } ?: DRAFT }
}

data class Author(val id: String, val name: String, val avatarUrl: String?, val country: String?)

data class Story(
    val id: String,
    val title: String,
    val excerpt: String,
    val coverImage: String?,
    val category: StoryCategory,
    val status: StoryStatus,
    val author: Author,
    val readingTime: Int,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val publishedAt: String?,
)

data class StoryDetail(
    val story: Story,
    val body: String,
    val rejectionReason: String,
    /** True when the article is being shown from the offline copy. */
    val isOfflineCopy: Boolean = false,
)

data class Comment(val id: String, val author: Author, val body: String, val createdAt: String)

enum class StoryFeed { LATEST, POPULAR, SAVED, MINE }

data class StoryFilter(val feed: StoryFeed = StoryFeed.LATEST, val category: StoryCategory? = null) {
    val listKey: String get() = "story:${feed.name}:${category?.apiValue ?: "all"}"
}

interface StoryRepository {
    fun paged(filter: StoryFilter): Flow<PagingData<Story>>
    fun featured(limit: Int = 5): Flow<List<Story>>
    suspend fun refreshFeatured(): AppResult<Unit>
    /** Emits the offline copy first (if any), then stays in sync with it. */
    fun observeDetail(id: String): Flow<StoryDetail?>
    suspend fun refreshDetail(id: String): AppResult<StoryDetail>
    fun offlineArticles(): Flow<List<Story>>
    suspend fun removeOffline(id: String)
    suspend fun setLiked(story: Story, liked: Boolean): SyncOutcome
    suspend fun setBookmarked(story: Story, bookmarked: Boolean): SyncOutcome
    suspend fun comments(storyId: String): AppResult<List<Comment>>
    suspend fun addComment(storyId: String, body: String): AppResult<Comment>
    suspend fun deleteStory(id: String): AppResult<Unit>
}
