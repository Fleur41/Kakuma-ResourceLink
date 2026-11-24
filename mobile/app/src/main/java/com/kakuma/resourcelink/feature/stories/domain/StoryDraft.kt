package com.kakuma.resourcelink.feature.stories.domain

import android.net.Uri
import com.kakuma.resourcelink.core.common.AppError
import kotlinx.coroutines.flow.Flow

enum class DraftState { LOCAL_ONLY, PENDING_UPLOAD, SYNCED }

data class StoryDraft(
    val localId: String,
    val remoteId: String?,
    val title: String,
    val body: String,
    val category: StoryCategory,
    val language: String,
    val coverPath: String?,
    val consentToPublish: Boolean,
    val state: DraftState,
    val updatedAt: Long,
)

sealed interface SubmitOutcome {
    /** Uploaded and sent to moderators. */
    data object SubmittedForReview : SubmitOutcome
    /** No connection: kept on the device and queued; it uploads automatically later. */
    data object QueuedOffline : SubmitOutcome
    data class Invalid(val fieldErrors: Map<String, String>) : SubmitOutcome
    data class Failed(val error: AppError) : SubmitOutcome
}

interface DraftRepository {
    fun drafts(): Flow<List<StoryDraft>>
    suspend fun get(localId: String): StoryDraft?
    /** Saves on the device only; always works offline. */
    suspend fun save(draft: StoryDraft): StoryDraft
    /** Copies the picked image into app-private storage and returns its path (scoped storage friendly). */
    suspend fun importCover(uri: Uri): String?
    suspend fun submit(localId: String): SubmitOutcome
    /** Uploads every queued draft; returns false if some must be retried. */
    suspend fun uploadPending(): Boolean
    suspend fun delete(localId: String)
}
