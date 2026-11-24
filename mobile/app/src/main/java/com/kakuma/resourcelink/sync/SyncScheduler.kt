package com.kakuma.resourcelink.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Single place that enqueues background work, always with a network constraint and exponential backoff. */
@Singleton
class SyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val workManager get() = WorkManager.getInstance(context)
    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Replays queued offline actions (bookmarks, likes, drafts) as soon as a connection is available. */
    fun schedulePendingOperations() {
        val request = OneTimeWorkRequestBuilder<PendingOperationsWorker>()
            .setConstraints(online)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: a newer action never cancels one that is already being sent.
        workManager.enqueueUniqueWork(PENDING_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** Periodic refresh of offline content and notification polling (no FCM: Firebase is not used). */
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(PENDING_WORK)
        workManager.cancelUniqueWork(PERIODIC_WORK)
    }

    companion object {
        const val PENDING_WORK = "pending_operations"
        const val PERIODIC_WORK = "periodic_sync"
    }
}
