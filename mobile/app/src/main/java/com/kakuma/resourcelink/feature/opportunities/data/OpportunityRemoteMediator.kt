package com.kakuma.resourcelink.feature.opportunities.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.database.entity.OpportunityEntity
import com.kakuma.resourcelink.core.database.entity.RemoteKeyEntity
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityFilter
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Network → Room → UI. Room is the single source of truth, so cached pages remain readable offline. */
@OptIn(ExperimentalPagingApi::class)
class OpportunityRemoteMediator(
    private val filter: OpportunityFilter,
    private val api: OpportunityApi,
    private val db: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : RemoteMediator<Int, OpportunityEntity>() {

    private val listKey = filter.listKey

    override suspend fun initialize(): InitializeAction {
        val key = db.remoteKeyDao().get(listKey)
        val fresh = key != null && now() - key.updatedAt < TimeUnit.MINUTES.toMillis(CACHE_MINUTES)
        return if (fresh) InitializeAction.SKIP_INITIAL_REFRESH else InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, OpportunityEntity>): MediatorResult {
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
                search = filter.query.trim().ifBlank { null },
                open = if (filter.openOnly) true else null,
                bookmarked = if (filter.bookmarkedOnly) true else null,
            )
            db.withTransaction {
                if (loadType == LoadType.REFRESH) db.opportunityDao().clearList(listKey)
                val start = db.opportunityDao().maxPosition(listKey) + 1
                db.opportunityDao().upsertAll(response.results.mapIndexed { i, dto -> dto.toEntity(listKey, start + i) })
                db.remoteKeyDao().upsert(RemoteKeyEntity(listKey, if (response.next != null) page + 1 else null, now()))
            }
            MediatorResult.Success(endOfPaginationReached = response.next == null)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private companion object { const val CACHE_MINUTES = 30L }
}
