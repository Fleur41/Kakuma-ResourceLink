package com.kakuma.resourcelink.feature.stories.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.database.entity.RemoteKeyEntity
import com.kakuma.resourcelink.core.database.entity.StoryEntity
import com.kakuma.resourcelink.feature.stories.domain.StoryFeed
import com.kakuma.resourcelink.feature.stories.domain.StoryFilter
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPagingApi::class)
class StoryRemoteMediator(
    private val filter: StoryFilter,
    private val api: StoryApi,
    private val db: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : RemoteMediator<Int, StoryEntity>() {
    private val listKey = filter.listKey

    override suspend fun initialize(): InitializeAction {
        val key = db.remoteKeyDao().get(listKey)
        // Personal lists change often; public feeds are cached for 15 minutes.
        val ttl = if (filter.feed == StoryFeed.MINE || filter.feed == StoryFeed.SAVED) 0L else TimeUnit.MINUTES.toMillis(15)
        return if (key != null && now() - key.updatedAt < ttl) InitializeAction.SKIP_INITIAL_REFRESH
        else InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, StoryEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> db.remoteKeyDao().get(listKey)?.nextPage
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        }
        return try {
            val response = api.list(
                page = page,
                pageSize = state.config.pageSize,
                category = filter.category?.apiValue,
                ordering = when (filter.feed) {
                    StoryFeed.POPULAR -> "-likes_count"
                    StoryFeed.MINE -> "-created_at"
                    else -> "-published_at"
                },
                bookmarked = if (filter.feed == StoryFeed.SAVED) true else null,
                mine = if (filter.feed == StoryFeed.MINE) true else null,
            )
            db.withTransaction {
                if (loadType == LoadType.REFRESH) db.storyDao().clearList(listKey)
                val start = db.storyDao().maxPosition(listKey) + 1
                db.storyDao().upsertAll(response.results.mapIndexed { i, dto -> dto.toEntity(listKey, start + i) })
                db.remoteKeyDao().upsert(RemoteKeyEntity(listKey, if (response.next != null) page + 1 else null, now()))
            }
            MediatorResult.Success(endOfPaginationReached = response.next == null)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}
