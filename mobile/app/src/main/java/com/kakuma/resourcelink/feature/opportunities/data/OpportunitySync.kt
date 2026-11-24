package com.kakuma.resourcelink.feature.opportunities.data

import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.database.entity.PendingOperationType
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityRepository
import com.kakuma.resourcelink.sync.OperationResult
import com.kakuma.resourcelink.sync.PendingOperationHandler
import com.kakuma.resourcelink.sync.SyncTask
import com.kakuma.resourcelink.sync.toOperationResult
import javax.inject.Inject

class OpportunityBookmarkHandler @Inject constructor(private val api: OpportunityApi) : PendingOperationHandler {
    override val types = setOf(PendingOperationType.BOOKMARK_OPPORTUNITY, PendingOperationType.UNBOOKMARK_OPPORTUNITY)

    override suspend fun execute(type: PendingOperationType, targetId: String): OperationResult {
        val response = if (type == PendingOperationType.BOOKMARK_OPPORTUNITY) api.bookmark(targetId) else api.unbookmark(targetId)
        return response.code().toOperationResult()
    }
}

class FeaturedOpportunitiesSyncTask @Inject constructor(private val repository: OpportunityRepository) : SyncTask {
    override val name = "featured_opportunities"
    override suspend fun run(): Boolean = repository.refreshFeatured() is AppResult.Success
}
