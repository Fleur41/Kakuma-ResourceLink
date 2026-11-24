package com.kakuma.resourcelink.core.ui

import com.kakuma.resourcelink.core.common.UiText
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide feedback for messages that must survive a screen change (e.g. "Signed in" right before the
 * login screen is replaced by Home, or "Session expired" when the session ends). Collected once in MainActivity.
 */
@Singleton
class AppMessages @Inject constructor() {
    private val channel = UiEventChannel()
    val events = channel.events
    fun toast(message: UiText) = channel.toast(message)
}
