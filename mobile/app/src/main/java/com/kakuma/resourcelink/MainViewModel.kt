package com.kakuma.resourcelink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.datastore.PreferencesRepository
import com.kakuma.resourcelink.core.datastore.ThemeMode
import com.kakuma.resourcelink.core.network.SessionEvents
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.notifications.domain.AppNotification
import com.kakuma.resourcelink.feature.notifications.domain.NotificationRepository
import com.kakuma.resourcelink.feature.notifications.domain.NotificationTarget
import com.kakuma.resourcelink.feature.notifications.domain.NotificationType
import com.kakuma.resourcelink.navigation.Routes
import com.kakuma.resourcelink.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppNavEvent {
    data object SessionExpired : AppNavEvent
    data class Open(val target: NotificationTarget) : AppNavEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val auth: AuthRepository,
    private val notifications: NotificationRepository,
    private val syncScheduler: SyncScheduler,
    sessionEvents: SessionEvents,
    val appMessages: AppMessages,
) : ViewModel() {

    /** Null until DataStore is read; the splash screen stays visible meanwhile (no flash of the wrong screen). */
    val startDestination: StateFlow<String?> = preferences.preferences
        .map { if (it.onboardingDone) Routes.HOME else Routes.WELCOME }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<ThemeMode> = preferences.preferences.map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val isLoggedIn = auth.isLoggedIn

    private val navEvents = Channel<AppNavEvent>(Channel.BUFFERED)
    val events = navEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionEvents.sessionExpired.collect {
                auth.logout() // tokens are already gone; wipe personal caches too
                appMessages.toast(UiText.Res(R.string.error_session_expired))
                navEvents.send(AppNavEvent.SessionExpired)
            }
        }
    }

    /** Fixes the start destination once, so later preference changes never rebuild the graph. */
    suspend fun awaitStart(): String = startDestination.first { it != null }!!

    fun completeOnboarding() = viewModelScope.launch { preferences.setOnboardingDone() }

    fun onSignedIn() {
        completeOnboarding()
        syncScheduler.schedulePendingOperations()
        viewModelScope.launch { notifications.refreshUnreadCount() }
    }

    /** Called with the extras of a tapped system notification. */
    fun onNotificationTapped(id: String?, type: String?, data: Map<String, String>) {
        if (type == null) return
        val n = AppNotification(id.orEmpty(), NotificationType.from(type), "", "", false, "", data)
        viewModelScope.launch {
            if (!id.isNullOrBlank()) notifications.markRead(id)
            navEvents.send(AppNavEvent.Open(n.target))
        }
    }
}
