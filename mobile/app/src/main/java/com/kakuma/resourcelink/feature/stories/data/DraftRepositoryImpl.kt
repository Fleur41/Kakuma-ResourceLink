package com.kakuma.resourcelink.feature.stories.data

import android.content.Context
import android.net.Uri
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.database.entity.DraftSyncState
import com.kakuma.resourcelink.core.database.entity.StoryDraftEntity
import com.kakuma.resourcelink.core.di.IoDispatcher
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.stories.domain.DraftRepository
import com.kakuma.resourcelink.feature.stories.domain.DraftState
import com.kakuma.resourcelink.feature.stories.domain.StoryCategory
import com.kakuma.resourcelink.feature.stories.domain.StoryDraft
import com.kakuma.resourcelink.feature.stories.domain.SubmitOutcome
import com.kakuma.resourcelink.sync.SyncScheduler
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun StoryDraftEntity.toDomain() = StoryDraft(
    localId, remoteId, title, body, StoryCategory.from(category), language, coverPath, consentToPublish,
    DraftState.valueOf(syncState.name), updatedAt,
)

private fun StoryDraft.toEntity() = StoryDraftEntity(
    localId, remoteId, title, body, category.apiValue, language, coverPath, consentToPublish,
    DraftSyncState.valueOf(state.name), updatedAt,
)

@Singleton
class DraftRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: StoryApi,
    private val db: AppDatabase,
    private val moshi: Moshi,
    private val syncScheduler: SyncScheduler,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DraftRepository {
    private val dao get() = db.storyDraftDao()
    /** One upload at a time, so a draft is never created twice on the server. */
    private val uploadLock = Mutex()

    override fun drafts(): Flow<List<StoryDraft>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(localId: String): StoryDraft? = dao.get(localId)?.toDomain()

    override suspend fun save(draft: StoryDraft): StoryDraft {
        val saved = draft.copy(
            localId = draft.localId.ifBlank { UUID.randomUUID().toString() },
            // Editing a synced/queued draft makes it local again until it is submitted.
            state = if (draft.state == DraftState.PENDING_UPLOAD) DraftState.PENDING_UPLOAD else DraftState.LOCAL_ONLY,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(saved.toEntity())
        return saved
    }

    override suspend fun importCover(uri: Uri): String? = withContext(io) {
        runCatching {
            val resolver = context.contentResolver
            val type = resolver.getType(uri) ?: "image/jpeg"
            if (!type.startsWith("image/")) return@runCatching null
            val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            if (size > MAX_COVER_BYTES) return@runCatching null
            val dir = File(context.filesDir, "draft_covers").apply { mkdirs() }
            val ext = when (type) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
            val file = File(dir, "${UUID.randomUUID()}.$ext")
            resolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return@runCatching null
            file.absolutePath
        }.getOrNull()
    }

    override suspend fun submit(localId: String): SubmitOutcome {
        val draft = dao.get(localId) ?: return SubmitOutcome.Failed(AppError.NotFound)
        dao.upsert(draft.copy(syncState = DraftSyncState.PENDING_UPLOAD, updatedAt = System.currentTimeMillis()))
        return uploadLock.withLock { upload(localId) }
    }

    private suspend fun upload(localId: String): SubmitOutcome {
        val draft = dao.get(localId) ?: return SubmitOutcome.Failed(AppError.NotFound)
        val request = StoryWriteRequest(draft.title.trim(), draft.body.trim(), draft.category, draft.language, draft.consentToPublish)
        val result = safeApiCall(moshi) {
            val story = if (draft.remoteId == null) api.create(request) else api.update(draft.remoteId, request)
            // Remember the server id immediately, so a retry updates instead of duplicating.
            dao.upsert(draft.copy(remoteId = story.id))
            draft.coverPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val mime = if (path.endsWith(".png")) "image/png" else if (path.endsWith(".webp")) "image/webp" else "image/jpeg"
                    api.uploadCover(story.id, MultipartBody.Part.createFormData("cover_image", file.name, file.asRequestBody(mime.toMediaType())))
                }
            }
            api.publish(story.id)
        }
        return when (result) {
            is AppResult.Success -> {
                dao.delete(localId)
                draft.coverPath?.let { File(it).delete() }
                SubmitOutcome.SubmittedForReview
            }
            is AppResult.Failure -> when (val e = result.error) {
                AppError.Network, is AppError.Server, AppError.RateLimited -> {
                    syncScheduler.schedulePendingOperations()
                    SubmitOutcome.QueuedOffline
                }
                is AppError.Validation -> {
                    dao.get(localId)?.let { dao.upsert(it.copy(syncState = DraftSyncState.LOCAL_ONLY)) }
                    SubmitOutcome.Invalid(e.fieldErrors)
                }
                else -> {
                    dao.get(localId)?.let { dao.upsert(it.copy(syncState = DraftSyncState.LOCAL_ONLY)) }
                    SubmitOutcome.Failed(e)
                }
            }
        }
    }

    override suspend fun uploadPending(): Boolean = uploadLock.withLock {
        dao.withState(DraftSyncState.PENDING_UPLOAD).map { upload(it.localId) }.none { it is SubmitOutcome.QueuedOffline }
    }

    override suspend fun delete(localId: String) {
        dao.get(localId)?.coverPath?.let { File(it).delete() }
        dao.delete(localId)
    }

    private companion object { const val MAX_COVER_BYTES = 5L * 1024 * 1024 }
}
