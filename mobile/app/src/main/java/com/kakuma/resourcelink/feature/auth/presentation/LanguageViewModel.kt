package com.kakuma.resourcelink.feature.auth.presentation

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.locale.AppLanguage
import com.kakuma.resourcelink.core.locale.LanguageManager
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager,
    private val authRepository: AuthRepository,
    private val appMessages: AppMessages,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _selected = MutableStateFlow(languageManager.current())
    val selected = _selected.asStateFlow()
    private var confirming = false

    fun select(language: AppLanguage) { _selected.value = language }

    /**
     * Applies the language on this phone; signed-in users also get it saved to their account. The phone switch
     * never waits on the network, but if the account update fails the toast says so instead of failing silently.
     */
    fun confirm(onDone: () -> Unit) {
        if (confirming) return
        confirming = true
        viewModelScope.launch {
            val language = _selected.value
            val changed = language != languageManager.current()
            val savedToAccount = !authRepository.isLoggedIn.value ||
                authRepository.updateProfile(null, null, null, language.tag) is AppResult.Success
            languageManager.apply(language)
            when {
                !savedToAccount -> toastIn(language, R.string.toast_language_changed_device_only)
                changed -> toastIn(language, R.string.toast_language_changed)
            }
            onDone()
        }
    }

    /**
     * Switching language recreates the activity, so the text is resolved in the new language up front and sent
     * through the app-wide [AppMessages], which survives the recreation.
     */
    private fun toastIn(language: AppLanguage, @StringRes res: Int) {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language.tag)) }
        val text = context.createConfigurationContext(config).getString(res, language.nativeName)
        appMessages.toast(UiText.Raw(text))
    }
}
