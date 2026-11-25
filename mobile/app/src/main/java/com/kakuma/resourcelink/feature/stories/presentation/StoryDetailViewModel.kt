package com.kakuma.resourcelink.feature.stories.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.ui.AppMessages
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.report
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.stories.domain.Comment
import com.kakuma.resourcelink.feature.stories.domain.StoryDetail
import com.kakuma.resourcelink.feature.stories.domain.StoryRepository
import com.kakuma.resourcelink.feature.stories.domain.StoryStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryDetailUiState(
    val detail: StoryDetail? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val isOwner: Boolean = false,
    val isLoggedIn: Boolean = false,
)

data class CommentsUiState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val draft: String = "",
    val isSending: Boolean = false,
)

@HiltViewModel
class StoryDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: StoryRepository,
    authRepository: AuthRepository,
    private val appMessages: AppMessages,
) : ViewModel() {
    val id: String = checkNotNull(savedState["id"])

    /** Server copy; for published stories the offline copy below mirrors it (and is all we have offline). */
    private val remote = MutableStateFlow<StoryDetail?>(null)
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<UiText?>(null)

    val state: StateFlow<StoryDetailUiState> = combine(
        repository.observeDetail(id), remote, loading, error, combine(authRepository.currentUser, authRepository.isLoggedIn, ::Pair),
    ) { offline, server, isLoading, err, (user, loggedIn) ->
        // Prefer the Room copy for published stories so like/bookmark toggles are reflected immediately.
        val detail = when {
            offline != null && server != null -> offline.copy(isOfflineCopy = false)
            else -> offline ?: server
        }
        StoryDetailUiState(
            detail = detail,
            isLoading = isLoading && detail == null,
            error = if (detail == null) err else null,
            isOwner = user != null && detail?.story?.author?.id == user.id,
            isLoggedIn = loggedIn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoryDetailUiState())

    private val _comments = MutableStateFlow(CommentsUiState())
    val comments: StateFlow<CommentsUiState> = _comments.asStateFlow()

    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            when (val r = repository.refreshDetail(id)) {
                is AppResult.Success -> {
                    remote.value = r.data
                    error.value = null
                    if (r.data.story.status == StoryStatus.PUBLISHED) loadComments()
                }
                is AppResult.Failure -> error.value = r.error.toUiText()
            }
            loading.value = false
        }
    }

    fun loadComments() {
        viewModelScope.launch {
            _comments.update { it.copy(isLoading = true, error = null) }
            when (val r = repository.comments(id)) {
                is AppResult.Success -> _comments.update { it.copy(comments = r.data, isLoading = false) }
                is AppResult.Failure -> _comments.update { it.copy(isLoading = false, error = r.error.toUiText()) }
            }
        }
    }

    fun onCommentChange(text: String) = _comments.update { it.copy(draft = text.take(2000)) }

    fun sendComment() {
        val text = _comments.value.draft.trim()
        if (text.isEmpty() || _comments.value.isSending) return
        _comments.update { it.copy(isSending = true) }
        viewModelScope.launch {
            when (val r = repository.addComment(id, text)) {
                is AppResult.Success -> {
                    _comments.update { it.copy(comments = it.comments + r.data, draft = "", isSending = false) }
                    uiEvents.toast(UiText.Res(R.string.toast_comment_posted))
                }
                is AppResult.Failure -> {
                    _comments.update { it.copy(isSending = false) }
                    uiEvents.snackbar(r.error.toUiText())
                }
            }
        }
    }

    fun toggleLike() {
        val story = state.value.detail?.story ?: return
        viewModelScope.launch {
            val outcome = repository.setLiked(story, !story.isLiked)
            remote.update { d -> d?.copy(story = d.story.copy(isLiked = !story.isLiked, likesCount = (story.likesCount + if (story.isLiked) -1 else 1).coerceAtLeast(0))) }
            uiEvents.report(outcome, successMessage = null)
        }
    }

    fun toggleBookmark() {
        val story = state.value.detail?.story ?: return
        viewModelScope.launch {
            val outcome = repository.setBookmarked(story, !story.isBookmarked)
            remote.update { d -> d?.copy(story = d.story.copy(isBookmarked = !story.isBookmarked)) }
            uiEvents.report(outcome, if (story.isBookmarked) R.string.toast_story_bookmark_removed else R.string.toast_story_bookmarked)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (val r = repository.deleteStory(id)) {
                is AppResult.Success -> { appMessages.toast(UiText.Res(R.string.toast_story_deleted)); onDeleted() }
                is AppResult.Failure -> uiEvents.snackbar(r.error.toUiText())
            }
        }
    }
}
