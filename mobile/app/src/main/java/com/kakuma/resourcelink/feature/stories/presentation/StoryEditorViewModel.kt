package com.kakuma.resourcelink.feature.stories.presentation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.locale.LanguageManager
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.stories.domain.DraftError
import com.kakuma.resourcelink.feature.stories.domain.DraftRepository
import com.kakuma.resourcelink.feature.stories.domain.DraftState
import com.kakuma.resourcelink.feature.stories.domain.StoryCategory
import com.kakuma.resourcelink.feature.stories.domain.StoryDraft
import com.kakuma.resourcelink.feature.stories.domain.StoryValidation
import com.kakuma.resourcelink.feature.stories.domain.SubmitOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryEditorUiState(
    val localId: String = "",
    val title: String = "",
    val body: String = "",
    val category: StoryCategory = StoryCategory.INSPIRATION,
    val language: String = "en",
    val coverPath: String? = null,
    val consent: Boolean = false,
    val errors: Map<String, UiText> = emptyMap(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isImporting: Boolean = false,
    val lastSavedAt: Long? = null,
) {
    val isEmpty: Boolean get() = title.isBlank() && body.isBlank() && coverPath == null
    val wordCount: Int get() = body.split(Regex("\\s+")).count { it.isNotBlank() }
}

fun DraftError.toUiText(): UiText = when (this) {
    DraftError.TITLE_TOO_SHORT -> UiText.Res(R.string.validation_title_length, listOf(StoryValidation.MIN_TITLE))
    DraftError.BODY_TOO_SHORT -> UiText.Res(R.string.validation_body_length, listOf(StoryValidation.MIN_BODY))
    DraftError.CONSENT_REQUIRED -> UiText.Res(R.string.validation_consent)
}

@OptIn(FlowPreview::class)
@HiltViewModel
class StoryEditorViewModel @Inject constructor(
    private val drafts: DraftRepository,
    languageManager: LanguageManager,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(StoryEditorUiState(language = languageManager.current().tag))
    val state: StateFlow<StoryEditorUiState> = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events
    private var autosave: Job? = null

    init {
        viewModelScope.launch {
            // "draftId" survives process death once the first autosave happened.
            val id: String? = savedState["draftId"]
            val draft = id?.let { drafts.get(it) }
            _state.update { s ->
                if (draft == null) s.copy(isLoading = false) else s.copy(
                    localId = draft.localId, title = draft.title, body = draft.body, category = draft.category,
                    language = draft.language, coverPath = draft.coverPath, consent = draft.consentToPublish,
                    isLoading = false, lastSavedAt = draft.updatedAt,
                )
            }
        }
    }

    fun onTitleChange(v: String) = edit { it.copy(title = v.take(StoryValidation.MAX_TITLE), errors = it.errors - "title") }
    fun onBodyChange(v: String) = edit { it.copy(body = v, errors = it.errors - "body") }
    fun onCategoryChange(c: StoryCategory) = edit { it.copy(category = c) }
    fun onLanguageChange(tag: String) = edit { it.copy(language = tag) }
    fun onConsentChange(v: Boolean) = edit { it.copy(consent = v, errors = it.errors - "consent") }
    fun removeCover() = edit { it.copy(coverPath = null) }

    fun onCoverPicked(uri: Uri?) {
        if (uri == null) return
        _state.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            val path = drafts.importCover(uri)
            if (path == null) {
                _state.update { it.copy(isImporting = false) }
                uiEvents.snackbar(UiText.Res(R.string.error_image_invalid))
            } else {
                edit { it.copy(coverPath = path, isImporting = false) }
            }
        }
    }

    private fun edit(transform: (StoryEditorUiState) -> StoryEditorUiState) {
        _state.update(transform)
        autosave?.cancel()
        autosave = viewModelScope.launch {
            delay(AUTOSAVE_MS)
            persist()
        }
    }

    private suspend fun persist(): StoryDraft? {
        val s = _state.value
        if (s.isEmpty && s.localId.isBlank()) return null
        val saved = drafts.save(
            StoryDraft(s.localId, null, s.title, s.body, s.category, s.language, s.coverPath, s.consent, DraftState.LOCAL_ONLY, 0),
        )
        savedState["draftId"] = saved.localId
        _state.update { it.copy(localId = saved.localId, lastSavedAt = saved.updatedAt) }
        return saved
    }

    fun saveDraft() {
        autosave?.cancel()
        viewModelScope.launch {
            if (persist() != null) uiEvents.toast(UiText.Res(R.string.toast_draft_saved))
        }
    }

    fun submit(onDone: () -> Unit) {
        val s = _state.value
        if (s.isSubmitting) return
        val errors = StoryValidation.forSubmit(s.title, s.body, s.consent)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors.mapValues { e -> e.value.toUiText() }) }
            return
        }
        autosave?.cancel()
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val draft = persist() ?: return@launch
            when (val outcome = drafts.submit(draft.localId)) {
                SubmitOutcome.SubmittedForReview -> { uiEvents.toast(UiText.Res(R.string.toast_story_submitted)); onDone() }
                SubmitOutcome.QueuedOffline -> { uiEvents.toast(UiText.Res(R.string.toast_story_queued)); onDone() }
                is SubmitOutcome.Invalid -> _state.update {
                    it.copy(isSubmitting = false, errors = outcome.fieldErrors.mapValues { e -> UiText.Raw(e.value) })
                }
                is SubmitOutcome.Failed -> {
                    _state.update { it.copy(isSubmitting = false) }
                    uiEvents.snackbar(outcome.error.toUiText())
                }
            }
        }
    }

    fun discard(onDone: () -> Unit) {
        autosave?.cancel()
        viewModelScope.launch {
            _state.value.localId.takeIf { it.isNotBlank() }?.let { drafts.delete(it) }
            uiEvents.toast(UiText.Res(R.string.toast_draft_deleted))
            onDone()
        }
    }

    /** Called when leaving the editor so nothing typed is lost. */
    fun flush() {
        if (autosave?.isActive == true) {
            autosave?.cancel()
            viewModelScope.launch { persist() }
        }
    }

    private companion object { const val AUTOSAVE_MS = 800L }
}
