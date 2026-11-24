package com.kakuma.resourcelink.feature.profile

import app.cash.turbine.test
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.datastore.PreferencesRepository
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.core.ui.UiEvent
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.sync.SyncScheduler
import com.kakuma.resourcelink.testutil.MainDispatcherRule
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = mockk<AuthRepository>(relaxed = true) { every { isLoggedIn } returns MutableStateFlow(true) }
    private val preferences = mockk<PreferencesRepository>(relaxed = true)
    private val appMessages = AppMessages()

    private fun viewModel() = ProfileViewModel(
        context = mockk(relaxed = true), auth = auth, preferences = preferences, languageManager = mockk(relaxed = true),
        syncScheduler = mockk<SyncScheduler>(relaxed = true), appMessages = appMessages, io = UnconfinedTestDispatcher(),
    )

    @Test fun `signing out clears the session, returns to welcome and shows a short app-wide toast`() = runTest {
        val vm = viewModel()

        appMessages.events.test {
            vm.signedOut.test {
                vm.logout()
                assertEquals(Unit, awaitItem()) // navigation leaves the signed-in screens
            }
            assertEquals(UiEvent.Toast(UiText.Res(R.string.toast_signed_out)), awaitItem())
        }
        // Next app start opens the welcome screen instead of Home.
        coVerifyOrder {
            auth.logout()
            preferences.resetOnboarding()
        }
    }
}
