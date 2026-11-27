package com.kakuma.resourcelink.feature.auth.presentation

import androidx.lifecycle.SavedStateHandle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgotPasswordViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()
    private val codeError = "That code is wrong or has expired."

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) = ForgotPasswordViewModel(repository, saved)

    private fun ForgotPasswordViewModel.toCodeStep() {
        coEvery { repository.requestPasswordReset(any()) } returns AppResult.Success(Unit)
        onEmailChange(" Amina@Example.com ")
        sendCode()
    }

    @Test fun `invalid email stays on the first step without calling the server`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("not-an-email")
        vm.sendCode()
        assertEquals(ResetStep.EMAIL, vm.state.value.step)
        assertTrue(vm.state.value.emailError != null)
        coVerify(exactly = 0) { repository.requestPasswordReset(any()) }
    }

    @Test fun `full reset sends the normalised email, the code and the new password`() = runTest {
        coEvery { repository.verifyPasswordReset(any(), any()) } returns AppResult.Success(Unit)
        coEvery { repository.confirmPasswordReset(any(), any(), any()) } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.toCodeStep()
        assertEquals(ResetStep.CODE, vm.state.value.step)

        vm.onCodeChange("Code: 482 913")
        assertEquals("482913", vm.state.value.code)
        vm.verifyCode()
        assertEquals(ResetStep.PASSWORD, vm.state.value.step)

        var done = false
        vm.onPasswordChange("An0ther-strong-pass")
        vm.onConfirmChange("An0ther-strong-pass")
        vm.savePassword { done = true }
        assertTrue(done)
        coVerify { repository.requestPasswordReset("amina@example.com") }
        coVerify { repository.verifyPasswordReset("amina@example.com", "482913") }
        coVerify { repository.confirmPasswordReset("amina@example.com", "482913", "An0ther-strong-pass") }
    }

    @Test fun `short code is rejected locally`() = runTest {
        val vm = viewModel()
        vm.toCodeStep()
        vm.onCodeChange("123")
        vm.verifyCode()
        assertEquals(UiText.Res(R.string.validation_code), vm.state.value.codeError)
        coVerify(exactly = 0) { repository.verifyPasswordReset(any(), any()) }
    }

    @Test fun `wrong code shows the server message`() = runTest {
        coEvery { repository.verifyPasswordReset(any(), any()) } returns
            AppResult.Failure(AppError.Validation(null, mapOf("code" to codeError)))
        val vm = viewModel()
        vm.toCodeStep()
        vm.onCodeChange("000000")
        vm.verifyCode()
        assertEquals(ResetStep.CODE, vm.state.value.step)
        assertEquals(UiText.Raw(codeError), vm.state.value.codeError)
    }

    @Test fun `mismatched passwords are caught before calling the server`() = runTest {
        coEvery { repository.verifyPasswordReset(any(), any()) } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.toCodeStep()
        vm.onCodeChange("482913")
        vm.verifyCode()
        vm.onPasswordChange("An0ther-strong-pass")
        vm.onConfirmChange("different-pass")
        vm.savePassword {}
        assertEquals(UiText.Res(R.string.validation_passwords_match), vm.state.value.confirmError)
        coVerify(exactly = 0) { repository.confirmPasswordReset(any(), any(), any()) }
    }

    @Test fun `code that expires while typing the password goes back to the code step`() = runTest {
        coEvery { repository.verifyPasswordReset(any(), any()) } returns AppResult.Success(Unit)
        coEvery { repository.confirmPasswordReset(any(), any(), any()) } returns
            AppResult.Failure(AppError.Validation(null, mapOf("code" to codeError)))
        val vm = viewModel()
        vm.toCodeStep()
        vm.onCodeChange("482913")
        vm.verifyCode()
        vm.onPasswordChange("An0ther-strong-pass")
        vm.onConfirmChange("An0ther-strong-pass")
        vm.savePassword {}
        assertEquals(ResetStep.CODE, vm.state.value.step)
        assertEquals(UiText.Raw(codeError), vm.state.value.codeError)
    }

    @Test fun `step and email survive process recreation`() = runTest {
        val saved = SavedStateHandle()
        viewModel(saved).toCodeStep()
        val restored = viewModel(saved)
        assertEquals(ResetStep.CODE, restored.state.value.step)
        assertEquals(" Amina@Example.com ", restored.state.value.email)
    }
}
