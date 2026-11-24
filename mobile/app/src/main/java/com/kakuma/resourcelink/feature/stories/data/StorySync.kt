package com.kakuma.resourcelink.feature.stories.data

import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.database.entity.PendingOperationType
import com.kakuma.resourcelink.core.database.entity.PendingOperationType.BOOKMARK_STORY
import com.kakuma.resourcelink.core.database.entity.PendingOperationType.LIKE_STORY
import com.kakuma.resourcelink.core.database.entity.PendingOperationType.UNBOOKMARK_STORY
import com.kakuma.resourcelink.core.database.entity.PendingOperationType.UNLIKE_STORY
import com.kakuma.resourcelink.feature.stories.domain.DraftRepository
import com.kakuma.resourcelink.feature.stories.domain.StoryRepository
import com.kakuma.resourcelink.sync.OperationResult
import com.kakuma.resourcelink.sync.PendingOperationHandler
import com.kakuma.resourcelink.sync.PendingUploader
import com.kakuma.resourcelink.sync.SyncTask
import com.kakuma.resourcelink.sync.toOperationResult
import javax.inject.Inject

class StoryActionHandler @Inject constructor(private val api: StoryApi) : PendingOperationHandler {
    override val types = setOf(LIKE_STORY, UNLIKE_STORY, BOOKMARK_STORY, UNBOOKMARK_STORY)

    override suspend fun execute(type: PendingOperationType, targetId: String): OperationResult = when (type) {
        LIKE_STORY -> api.like(targetId).code()
        UNLIKE_STORY -> api.unlike(targetId).code()
        BOOKMARK_STORY -> api.bookmark(targetId).code()
        else -> api.unbookmark(targetId).code()
    }.toOperationResult()
}

class DraftUploader @Inject constructor(private val drafts: DraftRepository) : PendingUploader {
    override suspend fun uploadPending(): Boolean = drafts.uploadPending()
}

class FeaturedStoriesSyncTask @Inject constructor(private val repository: StoryRepository) : SyncTask {
    override val name = "featured_stories"
    override suspend fun run(): Boolean = repository.refreshFeatured() is AppResult.Success
}
