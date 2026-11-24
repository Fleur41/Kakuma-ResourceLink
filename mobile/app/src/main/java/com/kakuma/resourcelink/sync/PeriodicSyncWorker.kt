package com.kakuma.resourcelink.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PeriodicSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tasks: Set<@JvmSuppressWildcards SyncTask>,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var anyFailed = false
        for (task in tasks) {
            val ok = runCatching { task.run() }.getOrElse { e ->
                Log.w(TAG, "Sync task ${task.name} failed: ${e.javaClass.simpleName}")
                false
            }
            if (!ok) anyFailed = true
        }
        return if (anyFailed && runAttemptCount < 3) Result.retry() else Result.success()
    }

    private companion object { const val TAG = "PeriodicSync" }
}
