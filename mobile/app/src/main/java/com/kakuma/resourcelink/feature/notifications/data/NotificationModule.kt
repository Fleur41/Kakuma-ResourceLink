package com.kakuma.resourcelink.feature.notifications.data

import com.kakuma.resourcelink.feature.notifications.domain.NotificationRepository
import com.kakuma.resourcelink.sync.SyncTask
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds @Singleton abstract fun repository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds @IntoSet abstract fun poller(impl: NotificationPoller): SyncTask

    companion object {
        @Provides @Singleton fun api(retrofit: Retrofit): NotificationApi = retrofit.create(NotificationApi::class.java)
    }
}
