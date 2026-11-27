package com.kakuma.resourcelink.feature.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.auth.domain.CredentialsValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ResetStep { EMAIL, CODE, PASSWORD }

data class ForgotPasswordUiState(
    val step: ResetStep = ResetStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirm: String = "",
    val emailError: UiText? = null,
    val codeError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmError: UiText? = null,
    val isLoading: Boolean = false,
    /** Seconds until another code can be requested (0 = allowed). */
    val resendIn: Int = 0,
)

/**
 * Password reset by emailed 6-digit code: email → code → new password.
 * The server answers the same whether or not the account exists, so the flow never reveals who has an account.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(
        // Keep the step and email if Android recreates the process while the person is reading the email.
        ForgotPasswordUiState(
            step = savedState.get<String>(KEY_STEP)?.let(ResetStep::valueOf) ?: ResetStep.EMAIL,
            email = savedState[KEY_EMAIL] ?: "",
        ),
    )
    val state = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events
    private var countdown: Job? = null

    private val email get() = _state.value.email.trim().lowercase()

    fun onEmailChange(v: String) {
        savedState[KEY_EMAIL] = v
        _state.update { it.copy(email = v, emailError = null) }
    }

    /** Digits only, at most 6, so pasted text like "Code: 482 913" still works. */
    fun onCodeChange(v: String) = _state.update { it.copy(code = v.filter(Char::isDigit).take(CODE_LENGTH), codeError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirm = v, confirmError = null) }

    fun sendCode() {
        if (_state.value.isLoading) return
        CredentialsValidator.email(_state.value.email)?.let { err -> _state.update { it.copy(emailError = err.toUiText()) }; return }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.requestPasswordReset(email)) {
                is AppResult.Success -> {
                    goTo(ResetStep.CODE)
                    _state.update { it.copy(isLoading = false) }
                    startCountdown()
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                    uiEvents.snackbar(result.error.toUiText())
                }
            }
        }
    }

    fun resendCode() {
        if (_state.value.resendIn > 0 || _state.value.isLoading) return
        viewModelScope.launch {
            when (val result = repository.requestPasswordReset(email)) {
                is AppResult.Success -> {
                    _state.update { it.copy(code = "", codeError = null) }
                    uiEvents.toast(UiText.Res(R.string.code_resent))
                    startCountdown()
                }
                is AppResult.Failure -> uiEvents.snackbar(result.error.toUiText())
            }
        }
    }

    fun changeEmail() {
        countdown?.cancel()
        goTo(ResetStep.EMAIL)
        _state.update { it.copy(code = "", codeError = null, resendIn = 0) }
    }

    fun verifyCode() {
        val s = _state.value
        if (s.isLoading) return
        if (s.code.length != CODE_LENGTH) { _state.update { it.copy(codeError = UiText.Res(R.string.validation_code)) }; return }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.verifyPasswordReset(email, s.code)) {
                is AppResult.Success -> {
                    goTo(ResetStep.PASSWORD)
                    _state.update { it.copy(isLoading = false) }
                }
                is AppResult.Failure -> handleFailure(result.error)
            }
        }
    }

    fun savePassword(onDone: () -> Unit) {
        val s = _state.value
        if (s.isLoading) return
        val passwordError = CredentialsValidator.newPassword(s.password)?.toUiText()
        val confirmError = if (passwordError == null && s.confirm != s.password) UiText.Res(R.string.validation_passwords_match) else null
        if (passwordError != null || confirmError != null) {
            _state.update { it.copy(passwordError = passwordError, confirmError = confirmError) }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.confirmPasswordReset(email, s.code, s.password)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    onDone()
                }
                is AppResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun handleFailure(error: AppError) {
        val fields = (error as? AppError.Validation)?.fieldErrors.orEmpty()
        when {
            "code" in fields -> {
                // Wrong or expired code, or it expired while the person typed a password: back to the code step.
                goTo(ResetStep.CODE)
                _state.update { it.copy(isLoading = false, codeError = UiText.Raw(fields.getValue("code"))) }
            }
            "new_password" in fields -> _state.update { it.copy(isLoading = false, passwordError = UiText.Raw(fields.getValue("new_password"))) }
            else -> {
                _state.update { it.copy(isLoading = false) }
                uiEvents.snackbar(error.toUiText())
            }
        }
    }

    private fun goTo(step: ResetStep) {
        savedState[KEY_STEP] = step.name
        _state.update { it.copy(step = step) }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (s in RESEND_SECONDS downTo 0) {
                _state.update { it.copy(resendIn = s) }
                if (s > 0) delay(1_000)
            }
        }
    }

    companion object {
        const val CODE_LENGTH = 6
        /** Matches PASSWORD_RESET_RESEND_SECONDS on the server. */
        const val RESEND_SECONDS = 60
        private const val KEY_STEP = "reset_step"
        private const val KEY_EMAIL = "reset_email"
    }
}
