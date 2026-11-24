package com.kakuma.resourcelink.feature.notifications.data

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kakuma.resourcelink.MainActivity
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.security.TokenStore
import com.kakuma.resourcelink.feature.notifications.domain.AppNotification
import com.kakuma.resourcelink.feature.notifications.domain.NotificationRepository
import com.kakuma.resourcelink.feature.notifications.domain.NotificationType
import com.kakuma.resourcelink.notifications.NotificationChannels
import com.kakuma.resourcelink.sync.SyncTask
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Polls unread notifications (Firebase/FCM is not used) and shows system notifications for ones not shown before.
 * Shown ids are remembered so the same notification is never posted twice.
 */
class NotificationPoller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NotificationRepository,
    private val tokenStore: TokenStore,
) : SyncTask {
    override val name = "notifications"
    private val prefs get() = context.getSharedPreferences("notification_poller", Context.MODE_PRIVATE)

    override suspend fun run(): Boolean {
        if (tokenStore.tokens() == null) return true
        repository.refreshUnreadCount()
        val unread = when (val r = repository.unread()) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return false
        }
        val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty()
        val fresh = unread.filter { it.id !in shown }
        if (fresh.isNotEmpty() && canPost()) fresh.forEach(::post)
        // Keep the set bounded: only ids that are still unread need remembering.
        prefs.edit().putStringSet(KEY_SHOWN, (shown + fresh.map { it.id }).intersect(unread.map { it.id }.toSet())).apply()
        return true
    }

    private fun canPost(): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @android.annotation.SuppressLint("MissingPermission") // checked in canPost()
    private fun post(n: AppNotification) {
        val channel = when (n.type) {
            NotificationType.MENTORSHIP_REQUEST, NotificationType.MENTORSHIP_UPDATE -> NotificationChannels.MENTORSHIP
            NotificationType.STORY_PUBLISHED, NotificationType.STORY_REJECTED, NotificationType.STORY_COMMENT -> NotificationChannels.STORIES
            NotificationType.PROJECT_CONTRIBUTION, NotificationType.PROJECT_STATUS -> NotificationChannels.COMMUNITY
            NotificationType.SYSTEM -> NotificationChannels.GENERAL
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NOTIFICATION_ID, n.id)
            putExtra(MainActivity.EXTRA_NOTIFICATION_TYPE, n.type.apiValue)
            n.data.forEach { (k, v) -> putExtra("data_$k", v) }
        }
        val pending = PendingIntent.getActivity(context, n.id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(ContextCompat.getColor(context, R.color.brand_primary))
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body)) // rich, expandable body
            .setCategory(if (channel == NotificationChannels.MENTORSHIP) NotificationCompat.CATEGORY_SOCIAL else NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setGroup(GROUP)
            .build()
        NotificationManagerCompat.from(context).notify(n.id.hashCode(), notification)
    }

    private companion object {
        const val KEY_SHOWN = "shown_ids"
        const val GROUP = "kakuma_updates"
    }
}
