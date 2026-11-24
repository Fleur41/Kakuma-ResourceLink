package com.kakuma.resourcelink.feature.stories.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.database.entity.PendingOperationType
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.opportunities.domain.SyncOutcome
import com.kakuma.resourcelink.feature.stories.domain.Comment
import com.kakuma.resourcelink.feature.stories.domain.Story
import com.kakuma.resourcelink.feature.stories.domain.StoryDetail
import com.kakuma.resourcelink.feature.stories.domain.StoryFilter
import com.kakuma.resourcelink.feature.stories.domain.StoryRepository
import com.kakuma.resourcelink.feature.stories.domain.StoryStatus
import com.kakuma.resourcelink.sync.SyncScheduler
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val api: StoryApi,
    private val db: AppDatabase,
    private val moshi: Moshi,
    private val syncScheduler: SyncScheduler,
) : StoryRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun paged(filter: StoryFilter): Flow<PagingData<Story>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        remoteMediator = StoryRemoteMediator(filter, api, db),
        pagingSourceFactory = { db.storyDao().pagingSource(filter.listKey) },
    ).flow.map { page -> page.map { it.toDomain() } }

    override fun featured(limit: Int): Flow<List<Story>> =
        db.storyDao().observeList(STORY_FEATURED_LIST, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshFeatured(): AppResult<Unit> =
        safeApiCall(moshi) { api.list(page = 1, pageSize = 5, ordering = "-published_at") }.map { page ->
            db.withTransaction {
                db.storyDao().clearList(STORY_FEATURED_LIST)
                db.storyDao().upsertAll(page.results.mapIndexed { i, dto -> dto.toEntity(STORY_FEATURED_LIST, i) })
            }
        }

    override fun observeDetail(id: String): Flow<StoryDetail?> =
        db.offlineArticleDao().observe(id).map { it?.toDetail() }

    override suspend fun refreshDetail(id: String): AppResult<StoryDetail> =
        safeApiCall(moshi) { api.get(id) }.map { dto ->
            // Every published article that is opened is kept for offline reading.
            if (StoryStatus.from(dto.status) == StoryStatus.PUBLISHED) {
                db.offlineArticleDao().upsert(dto.toOfflineArticle(System.currentTimeMillis()))
            }
            dto.toDetail()
        }

    override fun offlineArticles(): Flow<List<Story>> =
        db.offlineArticleDao().observeAll().map { list -> list.map { it.toStory() } }

    override suspend fun removeOffline(id: String) = db.offlineArticleDao().delete(id)

    override suspend fun setLiked(story: Story, liked: Boolean): SyncOutcome {
        val optimisticCount = (story.likesCount + if (liked) 1 else -1).coerceAtLeast(0)
        writeLike(story.id, liked, optimisticCount)
        val result = safeApiCall(moshi) {
            val response = if (liked) api.like(story.id) else api.unlike(story.id)
            if (!response.isSuccessful) throw HttpException(response)
            response.body()
        }
        return when (result) {
            is AppResult.Success -> {
                result.data?.let { writeLike(story.id, it.liked, it.likesCount) } // server count is authoritative
                SyncOutcome.Synced
            }
            is AppResult.Failure -> queueOrRevert(
                result.error, story.id,
                if (liked) PendingOperationType.LIKE_STORY else PendingOperationType.UNLIKE_STORY,
                if (liked) PendingOperationType.UNLIKE_STORY else PendingOperationType.LIKE_STORY,
            ) { writeLike(story.id, story.isLiked, story.likesCount) }
        }
    }

    override suspend fun setBookmarked(story: Story, bookmarked: Boolean): SyncOutcome {
        writeBookmark(story.id, bookmarked)
        val result = safeApiCall(moshi) {
            val response = if (bookmarked) api.bookmark(story.id) else api.unbookmark(story.id)
            if (!response.isSuccessful) throw HttpException(response)
        }
        return when (result) {
            is AppResult.Success -> SyncOutcome.Synced
            is AppResult.Failure -> queueOrRevert(
                result.error, story.id,
                if (bookmarked) PendingOperationType.BOOKMARK_STORY else PendingOperationType.UNBOOKMARK_STORY,
                if (bookmarked) PendingOperationType.UNBOOKMARK_STORY else PendingOperationType.BOOKMARK_STORY,
            ) { writeBookmark(story.id, !bookmarked) }
        }
    }

    private suspend fun queueOrRevert(
        error: AppError, id: String, type: PendingOperationType, opposite: PendingOperationType, revert: suspend () -> Unit,
    ): SyncOutcome = if (error == AppError.Network) {
        db.pendingOperationDao().enqueue(type, opposite, id, System.currentTimeMillis())
        syncScheduler.schedulePendingOperations()
        SyncOutcome.QueuedOffline
    } else {
        revert()
        SyncOutcome.Failed(error)
    }

    private suspend fun writeLike(id: String, liked: Boolean, count: Int) {
        db.storyDao().setLiked(id, liked, count)
        db.offlineArticleDao().setLiked(id, liked, count)
    }

    private suspend fun writeBookmark(id: String, bookmarked: Boolean) {
        db.storyDao().setBookmarked(id, bookmarked)
        db.offlineArticleDao().setBookmarked(id, bookmarked)
    }

    override suspend fun comments(storyId: String): AppResult<List<Comment>> =
        safeApiCall(moshi) { api.comments(storyId) }.map { page -> page.results.map { it.toDomain() } }

    override suspend fun addComment(storyId: String, body: String): AppResult<Comment> =
        safeApiCall(moshi) { api.addComment(storyId, CommentRequest(body.trim())) }.map { it.toDomain() }

    override suspend fun deleteStory(id: String): AppResult<Unit> = safeApiCall(moshi) {
        val response = api.delete(id)
        if (!response.isSuccessful) throw HttpException(response)
        db.offlineArticleDao().delete(id)
    }
}
