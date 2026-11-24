package com.kakuma.resourcelink.feature.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.auth.domain.AuthOutcome
import com.kakuma.resourcelink.feature.auth.domain.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val errors: Map<String, UiText> = emptyMap(),
    val isLoading: Boolean = false,
    /** Set once signed in; the screen then leaves the auth flow. */
    val completed: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase,
    private val appMessages: AppMessages,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    // The email survives process death; the password intentionally does not.
    private val _state = MutableStateFlow(LoginUiState(email = savedState[KEY_EMAIL] ?: ""))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    fun onEmailChange(value: String) {
        savedState[KEY_EMAIL] = value
        _state.update { it.copy(email = value, errors = it.errors - "email") }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, errors = it.errors - "password") }

    fun submit() {
        val current = _state.value
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true, errors = emptyMap()) }
        viewModelScope.launch {
            when (val outcome = login(current.email, current.password)) {
                is AuthOutcome.Success -> {
                    appMessages.toast(UiText.Res(R.string.toast_signed_in, listOf(outcome.user.firstName)))
                    _state.update { it.copy(isLoading = false, password = "", completed = true) }
                }
                is AuthOutcome.InvalidFields -> _state.update {
                    it.copy(isLoading = false, errors = fieldErrors(outcome.local, outcome.server))
                }
                is AuthOutcome.Failed -> {
                    _state.update { it.copy(isLoading = false) }
                    val message = if (outcome.error == AppError.Unauthorized) UiText.Res(R.string.error_bad_credentials)
                    else outcome.error.toUiText()
                    if (outcome.error == AppError.Network) {
                        uiEvents.snackbar(message, UiText.Res(R.string.action_retry), ACTION_RETRY)
                    } else {
                        uiEvents.snackbar(message)
                    }
                }
            }
        }
    }

    fun onAction(actionId: String) { if (actionId == ACTION_RETRY) submit() }

    private companion object {
        const val KEY_EMAIL = "email"
        const val ACTION_RETRY = "retry"
    }
}
