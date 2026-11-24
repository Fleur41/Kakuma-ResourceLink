package com.kakuma.resourcelink.feature.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.locale.AppLanguage
import com.kakuma.resourcelink.core.locale.LanguageManager
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.auth.domain.AuthOutcome
import com.kakuma.resourcelink.feature.auth.domain.RegisterUseCase
import com.kakuma.resourcelink.feature.auth.domain.Registration
import com.kakuma.resourcelink.feature.auth.domain.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole = UserRole.MEMBER,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val errors: Map<String, UiText> = emptyMap(),
    val isLoading: Boolean = false,
    val completed: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val register: RegisterUseCase,
    private val appMessages: AppMessages,
    languageManager: LanguageManager,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RegisterUiState(
            fullName = savedState["name"] ?: "",
            email = savedState["email"] ?: "",
            role = savedState.get<String>("role")?.let(UserRole::from) ?: UserRole.MEMBER,
            language = languageManager.current(),
        ),
    )
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    fun onNameChange(v: String) { savedState["name"] = v; _state.update { it.copy(fullName = v, errors = it.errors - "full_name") } }
    fun onEmailChange(v: String) { savedState["email"] = v; _state.update { it.copy(email = v, errors = it.errors - "email") } }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, errors = it.errors - "password") }
    fun onRoleChange(role: UserRole) { savedState["role"] = role.apiValue; _state.update { it.copy(role = role) } }
    fun onLanguageChange(language: AppLanguage) = _state.update { it.copy(language = language) }

    fun submit() {
        val s = _state.value
        if (s.isLoading) return
        _state.update { it.copy(isLoading = true, errors = emptyMap()) }
        viewModelScope.launch {
            when (val outcome = register(Registration(s.fullName, s.email, s.password, s.role, s.language.tag))) {
                is AuthOutcome.Success -> {
                    appMessages.toast(UiText.Res(R.string.toast_account_created))
                    _state.update { it.copy(isLoading = false, password = "", completed = true) }
                }
                is AuthOutcome.InvalidFields -> _state.update { it.copy(isLoading = false, errors = fieldErrors(outcome.local, outcome.server)) }
                is AuthOutcome.Failed -> {
                    _state.update { it.copy(isLoading = false) }
                    uiEvents.snackbar(outcome.error.toUiText())
                }
            }
        }
    }
}
