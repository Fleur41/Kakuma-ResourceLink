package com.kakuma.resourcelink.core.di

import android.content.Context
import androidx.room.Room
import com.kakuma.resourcelink.core.connectivity.ConnectivityObserver
import com.kakuma.resourcelink.core.connectivity.NetworkConnectivityObserver
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.datastore.DataStorePreferencesRepository
import com.kakuma.resourcelink.core.datastore.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "kakuma_resourcelink.db").build()

    @Provides fun opportunityDao(db: AppDatabase) = db.opportunityDao()
    @Provides fun remoteKeyDao(db: AppDatabase) = db.remoteKeyDao()
    @Provides fun storyDao(db: AppDatabase) = db.storyDao()
    @Provides fun offlineArticleDao(db: AppDatabase) = db.offlineArticleDao()
    @Provides fun storyDraftDao(db: AppDatabase) = db.storyDraftDao()
    @Provides fun pendingOperationDao(db: AppDatabase) = db.pendingOperationDao()
    @Provides fun serviceDao(db: AppDatabase) = db.serviceDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds @Singleton
    abstract fun preferences(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds @Singleton
    abstract fun connectivity(impl: NetworkConnectivityObserver): ConnectivityObserver
}
