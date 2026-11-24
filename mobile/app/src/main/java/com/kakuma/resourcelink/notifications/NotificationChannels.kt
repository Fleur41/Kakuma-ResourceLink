package com.kakuma.resourcelink.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.kakuma.resourcelink.R

/** User-visible channels so people can mute categories individually in system settings. */
object NotificationChannels {
    const val OPPORTUNITIES = "opportunities"
    const val STORIES = "stories"
    const val MENTORSHIP = "mentorship"
    const val COMMUNITY = "community"
    const val GENERAL = "general"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(OPPORTUNITIES, context.getString(R.string.channel_opportunities), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_opportunities_desc) },
            NotificationChannel(STORIES, context.getString(R.string.channel_stories), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_stories_desc) },
            NotificationChannel(MENTORSHIP, context.getString(R.string.channel_mentorship), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.channel_mentorship_desc) },
            NotificationChannel(COMMUNITY, context.getString(R.string.channel_community), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_community_desc) },
            NotificationChannel(GENERAL, context.getString(R.string.channel_general), NotificationManager.IMPORTANCE_LOW)
                .apply { description = context.getString(R.string.channel_general_desc) },
        )
        manager.createNotificationChannels(channels)
    }
}
