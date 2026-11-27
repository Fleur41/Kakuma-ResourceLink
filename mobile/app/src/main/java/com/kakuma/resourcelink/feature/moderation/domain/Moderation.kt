package com.kakuma.resourcelink.feature.moderation.domain

import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.feature.projects.domain.CommunityProject
import com.kakuma.resourcelink.feature.stories.domain.StoryDetail

/** The two review queues moderators work through (same as the web Moderation page). */
enum class ReviewQueue { STORIES, PROJECTS }

/** A story waiting for approval. [submittedAt] is an ISO-8601 timestamp, or null when unknown. */
data class ReviewStory(val detail: StoryDetail, val submittedAt: String?)

data class ReviewProject(val project: CommunityProject, val submittedAt: String?)

object ModerationRules {
    /** The backend requires 3–1000 characters for a story rejection reason; project notes are optional. */
    const val MIN_REASON = 3
    const val MAX_NOTE = 1000

    fun isReasonValid(reason: String) = reason.trim().length >= MIN_REASON
}

interface ModerationRepository {
    /** Oldest first, so the longest-waiting submissions are reviewed first. */
    suspend fun pendingStories(): AppResult<List<ReviewStory>>
    suspend fun pendingProjects(): AppResult<List<ReviewProject>>
    suspend fun approveStory(id: String): AppResult<Unit>
    suspend fun rejectStory(id: String, reason: String): AppResult<Unit>
    suspend fun decideProject(id: String, approve: Boolean, note: String): AppResult<Unit>
}
