package com.kakuma.resourcelink.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.auth.domain.CredentialsValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val error: UiText? = null,
    val isLoading: Boolean = false,
    val sent: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null) }

    fun submit() {
        val email = _state.value.email
        CredentialsValidator.email(email)?.let { err -> _state.update { it.copy(error = err.toUiText()) }; return }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.requestPasswordReset(email)) {
                is AppResult.Success -> _state.update { it.copy(isLoading = false, sent = true) }
                is AppResult.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                    uiEvents.snackbar(result.error.toUiText())
                }
            }
        }
    }
}
