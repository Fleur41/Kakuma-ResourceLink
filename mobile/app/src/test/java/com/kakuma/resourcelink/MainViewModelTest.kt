package com.kakuma.resourcelink

import app.cash.turbine.test
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.datastore.PreferencesRepository
import com.kakuma.resourcelink.core.datastore.UserPreferences
import com.kakuma.resourcelink.core.network.SessionEvents
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.core.ui.UiEvent
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.notifications.domain.NotificationRepository
import com.kakuma.resourcelink.navigation.Routes
import com.kakuma.resourcelink.sync.SyncScheduler
import com.kakuma.resourcelink.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val prefs = MutableStateFlow(UserPreferences())
    private val preferences = mockk<PreferencesRepository>(relaxed = true) { every { preferences } returns prefs }
    private val auth = mockk<AuthRepository>(relaxed = true) { every { isLoggedIn } returns MutableStateFlow(true) }
    private val notifications = mockk<NotificationRepository>(relaxed = true)
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val sessionEvents = SessionEvents()
    private val appMessages = AppMessages()

    private fun viewModel() = MainViewModel(preferences, auth, notifications, scheduler, sessionEvents, appMessages)

    @Test fun `start destination follows onboarding state`() = runTest {
        assertEquals(Routes.WELCOME, viewModel().awaitStart())

        prefs.value = UserPreferences(onboardingDone = true)
        assertEquals(Routes.HOME, viewModel().awaitStart())
    }

    @Test fun `expired session logs out, shows a short toast and returns to login`() = runTest {
        val vm = viewModel()

        appMessages.events.test {
            vm.events.test {
                sessionEvents.notifyExpired()
                assertEquals(AppNavEvent.SessionExpired, awaitItem())
            }
            assertEquals(UiEvent.Toast(UiText.Res(R.string.error_session_expired)), awaitItem())
        }
        coVerify(exactly = 1) { auth.logout() }
    }

    @Test fun `signing in completes onboarding and flushes offline actions`() = runTest {
        viewModel().onSignedIn()

        coVerify { preferences.setOnboardingDone() }
        verify { scheduler.schedulePendingOperations() }
        coVerify { notifications.refreshUnreadCount() }
    }
}
