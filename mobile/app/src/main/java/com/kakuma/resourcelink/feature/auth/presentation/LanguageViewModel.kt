package com.kakuma.resourcelink.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.core.locale.AppLanguage
import com.kakuma.resourcelink.core.locale.LanguageManager
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _selected = MutableStateFlow(languageManager.current())
    val selected = _selected.asStateFlow()

    fun select(language: AppLanguage) { _selected.value = language }

    /** Applies the language; signed-in users also get it saved to their account (best effort). */
    fun confirm(onDone: () -> Unit) {
        viewModelScope.launch {
            val language = _selected.value
            if (authRepository.isLoggedIn.value) authRepository.updateProfile(null, null, null, language.tag)
            languageManager.apply(language)
            onDone()
        }
    }
}
