package com.kakuma.resourcelink.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.designsystem.KakumaTheme
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.feature.auth.presentation.LoginScreen
import com.kakuma.resourcelink.feature.auth.presentation.LoginUiState
import com.kakuma.resourcelink.feature.auth.presentation.WelcomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

/** Stateless screens and shared state components rendered under Robolectric. */
@RunWith(AndroidJUnit4::class)
class ScreenStatesTest {
    @get:Rule val compose = createComposeRule()
    private fun str(id: Int, vararg args: Any) = RuntimeEnvironment.getApplication().getString(id, *args)

    @Test fun `error state shows message and retries`() {
        var retries = 0
        compose.setContent { KakumaTheme { ErrorState("No internet", onRetry = { retries++ }) } }

        compose.onNodeWithText("No internet").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.action_retry)).performClick()

        assertEquals(1, retries)
    }

    @Test fun `empty state shows its action only when provided`() {
        var clicked = false
        compose.setContent {
            KakumaTheme {
                EmptyState(Icons.Outlined.Bookmarks, "Nothing saved yet", "Tap the bookmark", actionLabel = "Explore", onAction = { clicked = true })
            }
        }

        compose.onNodeWithText("Nothing saved yet").assertIsDisplayed()
        compose.onNodeWithText("Explore").performClick()
        assertEquals(true, clicked)
    }

    @Test fun `login shows inline field errors and forwards input`() {
        val typed = mutableListOf<String>()
        compose.setContent {
            KakumaTheme {
                LoginScreen(
                    state = LoginUiState(errors = mapOf("email" to UiText.Res(R.string.validation_email))),
                    onBack = null,
                    onEmailChange = { typed += it },
                    onPasswordChange = {},
                    onSubmit = {},
                    onRegister = {},
                    onForgotPassword = {},
                )
            }
        }

        compose.onNodeWithText(str(R.string.validation_email)).assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("login_email"))).performTextInput("a@b.co")
        assertEquals(listOf("a@b.co"), typed)
    }

    @Test fun `login button announces loading and ignores taps while loading`() {
        var submits = 0
        compose.setContent {
            KakumaTheme {
                LoginScreen(LoginUiState(isLoading = true), null, {}, {}, onSubmit = { submits++ }, {}, {})
            }
        }

        compose.onNodeWithTag("login_submit")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, str(R.string.cd_loading)))
            .performClick()
        assertEquals(0, submits)
    }

    @Test fun `welcome offers sign in and guest browsing`() {
        var signIn = 0
        var guest = 0
        compose.setContent { KakumaTheme { WelcomeScreen(onGetStarted = {}, onSignIn = { signIn++ }, onBrowseAsGuest = { guest++ }) } }

        compose.onNodeWithText(str(R.string.action_have_account)).performClick()
        compose.onNodeWithText(str(R.string.action_browse_guest)).performClick()

        assertEquals(1, signIn)
        assertEquals(1, guest)
    }
}
