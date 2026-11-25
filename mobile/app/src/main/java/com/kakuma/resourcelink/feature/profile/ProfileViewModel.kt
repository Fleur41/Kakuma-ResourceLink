package com.kakuma.resourcelink.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.datastore.PreferencesRepository
import com.kakuma.resourcelink.core.datastore.ThemeMode
import com.kakuma.resourcelink.core.di.IoDispatcher
import com.kakuma.resourcelink.core.locale.LanguageManager
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.auth.domain.User
import com.kakuma.resourcelink.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoggedIn: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageName: String = "",
    val isUploadingAvatar: Boolean = false,
    val isLoggingOut: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthRepository,
    private val preferences: PreferencesRepository,
    private val languageManager: LanguageManager,
    private val syncScheduler: SyncScheduler,
    private val appMessages: AppMessages,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ViewModel() {
    private val busy = MutableStateFlow(Pair(false, false))
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    /** Fires once after sign-out so navigation can leave the signed-in screens. */
    private val signedOutChannel = Channel<Unit>(Channel.BUFFERED)
    val signedOut = signedOutChannel.receiveAsFlow()

    val state: StateFlow<ProfileUiState> = combine(auth.currentUser, auth.isLoggedIn, preferences.preferences, busy) { user, loggedIn, prefs, (uploading, out) ->
        ProfileUiState(user, loggedIn, prefs.themeMode, languageManager.current().nativeName, uploading, out)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init { viewModelScope.launch { if (auth.isLoggedIn.value) auth.refreshCurrentUser() } }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }

    fun onAvatarPicked(uri: Uri?) {
        if (uri == null) return
        busy.update { it.copy(first = true) }
        viewModelScope.launch {
            val picked = withContext(io) {
                runCatching {
                    val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes == null || bytes.size > 5 * 1024 * 1024 || !type.startsWith("image/")) null else bytes to type
                }.getOrNull()
            }
            if (picked == null) {
                uiEvents.snackbar(UiText.Res(R.string.error_image_invalid))
            } else when (val r = auth.uploadAvatar(picked.first, picked.second)) {
                is AppResult.Success -> uiEvents.toast(UiText.Res(R.string.toast_photo_updated))
                is AppResult.Failure -> uiEvents.snackbar(r.error.toUiText())
            }
            busy.update { it.copy(first = false) }
        }
    }

    fun logout() {
        busy.update { it.copy(second = true) }
        viewModelScope.launch {
            auth.logout()
            preferences.resetOnboarding()
            syncScheduler.cancelAll()
            syncScheduler.schedulePeriodicSync() // public content still refreshes for guests
            // App-wide toast: the profile screen is replaced by the welcome screen right after this.
            appMessages.toast(UiText.Res(R.string.toast_signed_out))
            busy.update { it.copy(second = false) }
            signedOutChannel.send(Unit)
        }
    }
}

data class EditProfileUiState(
    val fullName: String = "",
    val bio: String = "",
    val location: String = "",
    val errors: Map<String, UiText> = emptyMap(),
    val isSaving: Boolean = false,
    val loaded: Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val appMessages: AppMessages,
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state = _state
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    init {
        viewModelScope.launch {
            auth.currentUser.collect { u ->
                if (u != null && !_state.value.loaded) _state.value = EditProfileUiState(u.fullName, u.bio, u.location, loaded = true)
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(fullName = v.take(150), errors = it.errors - "full_name") }
    fun onBio(v: String) = _state.update { it.copy(bio = v.take(1000)) }
    fun onLocation(v: String) = _state.update { it.copy(location = v.take(120)) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.fullName.trim().length < 2) { _state.update { it.copy(errors = mapOf("full_name" to UiText.Res(R.string.validation_name_length))) }; return }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val r = auth.updateProfile(s.fullName.trim(), s.bio.trim(), s.location.trim(), null)) {
                is AppResult.Success -> { appMessages.toast(UiText.Res(R.string.toast_profile_updated)); onDone() }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSaving = false) }
                    val e = r.error
                    if (e is AppError.Validation && e.fieldErrors.isNotEmpty()) _state.update { it.copy(errors = e.fieldErrors.mapValues { f -> UiText.Raw(f.value) }) }
                    else uiEvents.snackbar(e.toUiText())
                }
            }
        }
    }
}
