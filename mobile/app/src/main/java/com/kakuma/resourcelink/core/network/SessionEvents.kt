package com.kakuma.resourcelink.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Emits once when the session becomes unrecoverable so the UI can show a message and return to login. */
@Singleton
class SessionEvents @Inject constructor() {
    private val expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sessionExpired: SharedFlow<Unit> = expired.asSharedFlow()

    fun notifyExpired() {
        expired.tryEmit(Unit)
    }
}
