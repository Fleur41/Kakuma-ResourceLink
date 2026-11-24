package com.kakuma.resourcelink.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.security.TokenStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class PendingOperationsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase,
    private val tokenStore: TokenStore,
    private val handlers: Set<@JvmSuppressWildcards PendingOperationHandler>,
    private val uploaders: Set<@JvmSuppressWildcards PendingUploader>,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (tokenStore.tokens() == null) return Result.success() // signed out: nothing to send
        val dao = db.pendingOperationDao()
        var retry = false
        for (op in dao.all()) {
            val handler = handlers.firstOrNull { op.type in it.types }
            if (handler == null) { dao.deleteById(op.id); continue }
            val result = try {
                handler.execute(op.type, op.targetId)
            } catch (_: IOException) {
                OperationResult.RETRY
            }
            when (result) {
                OperationResult.DONE, OperationResult.DROP -> dao.deleteById(op.id)
                OperationResult.RETRY -> if (op.attempts >= MAX_ATTEMPTS) dao.deleteById(op.id) else {
                    dao.incrementAttempts(op.id); retry = true
                }
            }
        }
        for (uploader in uploaders) {
            val ok = try { uploader.uploadPending() } catch (_: IOException) { false }
            if (!ok) retry = true
        }
        return if (retry) Result.retry() else Result.success()
    }

    private companion object { const val MAX_ATTEMPTS = 8 }
}
