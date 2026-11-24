package com.kakuma.resourcelink.feature.opportunities.data

import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import com.kakuma.resourcelink.sync.PendingOperationHandler
import com.kakuma.resourcelink.sync.SyncTask
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OpportunityModule {
    @Binds @Singleton
    abstract fun repository(impl: OpportunityRepositoryImpl): OpportunityRepository

    @Binds @IntoSet
    abstract fun bookmarkHandler(impl: OpportunityBookmarkHandler): PendingOperationHandler

    @Binds @IntoSet
    abstract fun featuredTask(impl: FeaturedOpportunitiesSyncTask): SyncTask

    companion object {
        @Provides @Singleton
        fun api(retrofit: Retrofit): OpportunityApi = retrofit.create(OpportunityApi::class.java)
    }
}
