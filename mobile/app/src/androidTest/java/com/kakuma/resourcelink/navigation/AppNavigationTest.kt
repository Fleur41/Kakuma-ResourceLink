package com.kakuma.resourcelink.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.testing.WorkManagerTestInitHelper
import com.kakuma.resourcelink.HiltTestActivity
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.KakumaTheme
import com.kakuma.resourcelink.core.ui.LocalSnackbarHostState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real navigation graph with real Hilt bindings; verifies auth flow and member-only guards. */
@HiltAndroidTest
class AppNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var nav: NavHostController
    private var onboardingDone = false
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(id: Int) = context.getString(id)
    private val route get() = nav.currentDestination?.route

    @Before fun setUp() {
        hilt.inject()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    private fun launch(start: String, loggedIn: Boolean = false) {
        compose.setContent {
            nav = rememberNavController()
            CompositionLocalProvider(LocalSnackbarHostState provides SnackbarHostState()) {
                KakumaTheme {
                    AppNavHost(
                        navController = nav,
                        startDestination = start,
                        onOnboardingDone = { onboardingDone = true },
                        onSignedIn = {},
                        isLoggedIn = { loggedIn },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun welcomeToLoginAndBack() {
        launch(Routes.WELCOME)

        compose.onNodeWithText(str(R.string.action_have_account)).performClick()
        compose.waitForIdle()
        assertEquals(Routes.LOGIN, route)

        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        compose.waitForIdle()
        assertEquals(Routes.WELCOME, route)
    }

    @Test fun guestBrowsingOpensHomeAndClearsOnboarding() {
        launch(Routes.WELCOME)

        compose.onNodeWithText(str(R.string.action_browse_guest)).performClick()
        compose.waitForIdle()

        assertEquals(Routes.HOME, route)
        assertNull("welcome must not stay on the back stack", nav.previousBackStackEntry)
        assertTrue(onboardingDone)
    }

    @Test fun loginLinksToRegisterWithoutStackingScreens() {
        launch(Routes.LOGIN)

        compose.onNodeWithText(str(R.string.action_create_account)).performClick()
        compose.waitForIdle()

        assertEquals(Routes.REGISTER, route)
        assertNull(nav.previousBackStackEntry)
    }

    @Test fun guestOpeningMentorshipIsSentToLogin() {
        launch(Routes.COMMUNITY, loggedIn = false)

        compose.onNodeWithText(str(R.string.nav_mentorship)).performClick()
        compose.waitForIdle()

        assertEquals(Routes.LOGIN, route)
    }

    @Test fun memberOpensMentorship() {
        launch(Routes.COMMUNITY, loggedIn = true)

        compose.onNodeWithText(str(R.string.nav_mentorship)).performClick()
        compose.waitForIdle()

        assertEquals(Routes.MENTORSHIP, route)
    }

    @Test fun guestCannotStayOnAssistant() {
        launch(Routes.HOME, loggedIn = false)

        compose.runOnUiThread { nav.navigate(Routes.assistant()) }
        compose.waitForIdle()

        assertEquals(Routes.LOGIN, route)
        assertEquals(Routes.HOME, nav.previousBackStackEntry?.destination?.route)
    }
}
