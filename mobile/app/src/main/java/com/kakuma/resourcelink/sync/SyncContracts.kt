package com.kakuma.resourcelink.sync

import com.kakuma.resourcelink.core.database.entity.PendingOperationType

/** Result of replaying one queued action. */
enum class OperationResult {
    DONE,
    /** Temporary failure (offline, 5xx): keep it and retry later. */
    RETRY,
    /** Permanent failure (e.g. content deleted): drop it. */
    DROP,
}

/** Features contribute handlers for the offline actions they create (Hilt multibinding). */
interface PendingOperationHandler {
    val types: Set<PendingOperationType>
    suspend fun execute(type: PendingOperationType, targetId: String): OperationResult
}

/** Uploads that are not simple toggles (e.g. story drafts). Returns false if something must be retried. */
interface PendingUploader {
    suspend fun uploadPending(): Boolean
}

/** Features contribute periodic refresh tasks (featured content, services, notifications…). */
interface SyncTask {
    val name: String
    /** Returns false on a transient failure so the worker can retry. */
    suspend fun run(): Boolean
}

/** 2xx done; 4xx (except 408/429) can never succeed, so drop; everything else is retried. */
fun Int.toOperationResult(): OperationResult = when {
    this in 200..299 -> OperationResult.DONE
    this == 408 || this == 429 -> OperationResult.RETRY
    this in 400..499 -> OperationResult.DROP
    else -> OperationResult.RETRY
}
