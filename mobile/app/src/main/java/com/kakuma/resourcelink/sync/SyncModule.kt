package com.kakuma.resourcelink.sync

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Declares the (possibly empty) handler sets so the workers compile before every feature contributes. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Multibinds abstract fun handlers(): Set<PendingOperationHandler>
    @Multibinds abstract fun tasks(): Set<SyncTask>
    @Multibinds abstract fun uploaders(): Set<PendingUploader>
}
