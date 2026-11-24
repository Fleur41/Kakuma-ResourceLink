package com.kakuma.resourcelink.feature.auth.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.core.ui.UiEvent
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.auth.domain.LoginUseCase
import com.kakuma.resourcelink.testutil.MainDispatcherRule
import com.kakuma.resourcelink.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()
    private val appMessages = AppMessages()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        LoginViewModel(LoginUseCase(repository), appMessages, savedState)

    @Test fun `email is restored after process recreation but password is not`() {
        val saved = SavedStateHandle()
        viewModel(saved).apply {
            onEmailChange("amina@example.com")
            onPasswordChange("secret")
        }

        val restored = viewModel(saved).state.value

        assertEquals("amina@example.com", restored.email)
        assertEquals("", restored.password)
    }

    @Test fun `invalid input shows inline errors without a network call`() {
        val vm = viewModel()

        vm.submit()

        val state = vm.state.value
        assertEquals(UiText.Res(R.string.validation_required), state.errors["email"])
        assertEquals(UiText.Res(R.string.validation_required), state.errors["password"])
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test fun `typing clears that field's error only`() {
        val vm = viewModel()
        vm.submit()

        vm.onEmailChange("a")

        assertFalse("email" in vm.state.value.errors)
        assertTrue("password" in vm.state.value.errors)
    }

    @Test fun `loading state is shown while signing in and success completes with a short toast`() = runTest {
        val gate = CompletableDeferred<AppResult<com.kakuma.resourcelink.feature.auth.domain.User>>()
        coEvery { repository.login(any(), any()) } coAnswers { gate.await() }
        val vm = viewModel().apply { onEmailChange("amina@example.com"); onPasswordChange("secret") }

        appMessages.events.test {
            vm.submit()
            assertTrue(vm.state.value.isLoading)
            vm.submit() // double tap is ignored while loading
            coVerify(exactly = 1) { repository.login(any(), any()) }

            gate.complete(AppResult.Success(TestData.user("Amina Hassan")))

            assertEquals(UiEvent.Toast(UiText.Res(R.string.toast_signed_in, listOf("Amina"))), awaitItem())
            val state = vm.state.value
            assertTrue(state.completed)
            assertFalse(state.isLoading)
            assertEquals("", state.password)
        }
    }

    @Test fun `wrong credentials show a snackbar without retry`() = runTest {
        coEvery { repository.login(any(), any()) } returns AppResult.Failure(AppError.Unauthorized)
        val vm = viewModel().apply { onEmailChange("amina@example.com"); onPasswordChange("wrong") }

        vm.events.test {
            vm.submit()
            assertEquals(UiEvent.Snackbar(UiText.Res(R.string.error_bad_credentials)), awaitItem())
        }
        assertFalse(vm.state.value.completed)
    }

    @Test fun `offline failure offers retry and retry submits again`() = runTest {
        coEvery { repository.login(any(), any()) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel().apply { onEmailChange("amina@example.com"); onPasswordChange("secret") }

        vm.events.test {
            vm.submit()
            val event = awaitItem() as UiEvent.Snackbar
            assertEquals(UiText.Res(R.string.error_network), event.message)
            assertEquals(UiText.Res(R.string.action_retry), event.actionLabel)

            coEvery { repository.login(any(), any()) } returns AppResult.Success(TestData.user())
            vm.onAction(event.actionId!!)
        }
        assertTrue(vm.state.value.completed)
        coVerify(exactly = 2) { repository.login(any(), any()) }
    }
}
