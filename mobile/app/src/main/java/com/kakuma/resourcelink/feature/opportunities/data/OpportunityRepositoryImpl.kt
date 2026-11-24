package com.kakuma.resourcelink.feature.opportunities.data

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
import com.kakuma.resourcelink.feature.opportunities.domain.Opportunity
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityFilter
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityRepository
import com.kakuma.resourcelink.feature.opportunities.domain.SyncOutcome
import com.kakuma.resourcelink.sync.SyncScheduler
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpportunityRepositoryImpl @Inject constructor(
    private val api: OpportunityApi,
    private val db: AppDatabase,
    private val moshi: Moshi,
    private val syncScheduler: SyncScheduler,
) : OpportunityRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun paged(filter: OpportunityFilter): Flow<PagingData<Opportunity>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = OpportunityRemoteMediator(filter, api, db),
        pagingSourceFactory = { db.opportunityDao().pagingSource(filter.listKey) },
    ).flow.map { page -> page.map { it.toDomain() } }

    override fun featured(limit: Int): Flow<List<Opportunity>> =
        db.opportunityDao().observeList(FEATURED_LIST, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshFeatured(): AppResult<Unit> =
        safeApiCall(moshi) { api.list(page = 1, pageSize = 5, open = true) }.map { page ->
            db.withTransaction {
                db.opportunityDao().clearList(FEATURED_LIST)
                db.opportunityDao().upsertAll(page.results.mapIndexed { i, dto -> dto.toEntity(FEATURED_LIST, i) })
            }
        }

    override fun observe(id: String): Flow<Opportunity?> = db.opportunityDao().observe(id).map { it?.toDomain() }

    override suspend fun refresh(id: String): AppResult<Opportunity> =
        safeApiCall(moshi) { api.get(id) }.map { dto ->
            db.opportunityDao().upsertAll(listOf(dto.toEntity(DETAIL_LIST, 0)))
            db.opportunityDao().setBookmarked(id, dto.isBookmarked) // keep every cached copy consistent
            dto.toDomain()
        }

    override suspend fun setBookmarked(id: String, bookmarked: Boolean): SyncOutcome {
        db.opportunityDao().setBookmarked(id, bookmarked) // optimistic
        val result = safeApiCall(moshi) {
            val response = if (bookmarked) api.bookmark(id) else api.unbookmark(id)
            if (!response.isSuccessful) throw HttpException(response)
        }
        return when (result) {
            is AppResult.Success -> SyncOutcome.Synced
            is AppResult.Failure -> if (result.error == AppError.Network) {
                val (type, opposite) = if (bookmarked) {
                    PendingOperationType.BOOKMARK_OPPORTUNITY to PendingOperationType.UNBOOKMARK_OPPORTUNITY
                } else {
                    PendingOperationType.UNBOOKMARK_OPPORTUNITY to PendingOperationType.BOOKMARK_OPPORTUNITY
                }
                db.pendingOperationDao().enqueue(type, opposite, id, System.currentTimeMillis())
                syncScheduler.schedulePendingOperations()
                SyncOutcome.QueuedOffline
            } else {
                db.opportunityDao().setBookmarked(id, !bookmarked) // revert
                SyncOutcome.Failed(result.error)
            }
        }
    }

    private companion object { const val PAGE_SIZE = 20 }
}
