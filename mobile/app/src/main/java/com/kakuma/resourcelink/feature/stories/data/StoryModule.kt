package com.kakuma.resourcelink.feature.stories.data

import com.kakuma.resourcelink.feature.stories.domain.DraftRepository
import com.kakuma.resourcelink.feature.stories.domain.StoryRepository
import com.kakuma.resourcelink.sync.PendingOperationHandler
import com.kakuma.resourcelink.sync.PendingUploader
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
abstract class StoryModule {
    @Binds @Singleton abstract fun repository(impl: StoryRepositoryImpl): StoryRepository
    @Binds @Singleton abstract fun drafts(impl: DraftRepositoryImpl): DraftRepository
    @Binds @IntoSet abstract fun actions(impl: StoryActionHandler): PendingOperationHandler
    @Binds @IntoSet abstract fun uploader(impl: DraftUploader): PendingUploader
    @Binds @IntoSet abstract fun featured(impl: FeaturedStoriesSyncTask): SyncTask

    companion object {
        @Provides @Singleton
        fun api(retrofit: Retrofit): StoryApi = retrofit.create(StoryApi::class.java)
    }
}
