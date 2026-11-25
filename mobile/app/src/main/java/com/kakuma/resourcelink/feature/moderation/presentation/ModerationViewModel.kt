package com.kakuma.resourcelink.feature.moderation.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.moderation.domain.ModerationRepository
import com.kakuma.resourcelink.feature.moderation.domain.ModerationRules
import com.kakuma.resourcelink.feature.moderation.domain.ReviewProject
import com.kakuma.resourcelink.feature.moderation.domain.ReviewQueue
import com.kakuma.resourcelink.feature.moderation.domain.ReviewStory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QueueState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    /** True once the queue has loaded at least once (so a reload shows the pull-to-refresh spinner, not skeletons). */
    val loaded: Boolean = false,
)

/** The approve/reject dialog that is open, if any. */
data class PendingDecision(val queue: ReviewQueue, val id: String, val title: String, val approve: Boolean) {
    /** Rejecting a story needs a reason for the author; every other decision takes an optional note (or none). */
    val needsReason: Boolean get() = queue == ReviewQueue.STORIES && !approve
    val takesNote: Boolean get() = queue == ReviewQueue.PROJECTS || !approve
}

data class ModerationUiState(
    val queue: ReviewQueue = ReviewQueue.STORIES,
    val stories: QueueState<ReviewStory> = QueueState(),
    val projects: QueueState<ReviewProject> = QueueState(),
    val decision: PendingDecision? = null,
    val note: String = "",
    val noteError: UiText? = null,
    val isDeciding: Boolean = false,
)

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val repository: ModerationRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ModerationUiState(queue = savedState.get<String>(KEY_QUEUE)?.let(ReviewQueue::valueOf) ?: ReviewQueue.STORIES),
    )
    val state = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    init { load(_state.value.queue) }

    fun selectQueue(queue: ReviewQueue) {
        savedState[KEY_QUEUE] = queue.name
        _state.update { it.copy(queue = queue) }
        val current = if (queue == ReviewQueue.STORIES) _state.value.stories else _state.value.projects
        if (!current.loaded && !current.isLoading) load(queue)
    }

    fun refresh() = load(_state.value.queue)

    private fun load(queue: ReviewQueue) {
        viewModelScope.launch {
            when (queue) {
                ReviewQueue.STORIES -> {
                    _state.update { it.copy(stories = it.stories.copy(isLoading = true, error = null)) }
                    val r = repository.pendingStories()
                    _state.update { it.copy(stories = it.stories.applied(r)) }
                }
                ReviewQueue.PROJECTS -> {
                    _state.update { it.copy(projects = it.projects.copy(isLoading = true, error = null)) }
                    val r = repository.pendingProjects()
                    _state.update { it.copy(projects = it.projects.applied(r)) }
                }
            }
        }
    }

    fun openDecision(queue: ReviewQueue, id: String, title: String, approve: Boolean) =
        _state.update { it.copy(decision = PendingDecision(queue, id, title, approve), note = "", noteError = null) }

    fun dismissDecision() {
        if (_state.value.isDeciding) return
        _state.update { it.copy(decision = null, note = "", noteError = null) }
    }

    fun onNote(value: String) = _state.update { it.copy(note = value.take(ModerationRules.MAX_NOTE), noteError = null) }

    fun confirmDecision() {
        val s = _state.value
        val d = s.decision ?: return
        if (s.isDeciding) return
        if (d.needsReason && !ModerationRules.isReasonValid(s.note)) {
            _state.update { it.copy(noteError = UiText.Res(R.string.moderation_reason_required)) }
            return
        }
        _state.update { it.copy(isDeciding = true) }
        viewModelScope.launch {
            val result = when (d.queue) {
                ReviewQueue.STORIES -> if (d.approve) repository.approveStory(d.id) else repository.rejectStory(d.id, s.note)
                ReviewQueue.PROJECTS -> repository.decideProject(d.id, d.approve, s.note)
            }
            when (result) {
                is AppResult.Success -> {
                    uiEvents.toast(UiText.Res(if (d.approve) R.string.toast_approved else R.string.toast_rejected))
                    _state.update { it.withoutItem(d).copy(decision = null, note = "", isDeciding = false) }
                }
                is AppResult.Failure -> onDecisionFailed(d, result.error)
            }
        }
    }

    private fun onDecisionFailed(d: PendingDecision, error: AppError) {
        when {
            // Another moderator got there first: the item is no longer waiting for review.
            error == AppError.NotFound || error is AppError.Conflict -> {
                uiEvents.snackbar(UiText.Res(R.string.moderation_already_reviewed))
                _state.update { it.withoutItem(d).copy(decision = null, note = "", isDeciding = false) }
                load(d.queue)
            }
            error is AppError.Validation && error.fieldErrors.isNotEmpty() -> {
                val message = error.fieldErrors["reason"] ?: error.fieldErrors["note"] ?: error.fieldErrors.values.first()
                _state.update { it.copy(isDeciding = false, noteError = UiText.Raw(message)) }
            }
            else -> {
                _state.update { it.copy(isDeciding = false) }
                uiEvents.snackbar(error.toUiText())
            }
        }
    }

    private companion object {
        const val KEY_QUEUE = "queue"
    }
}

private fun <T> QueueState<T>.applied(result: AppResult<List<T>>): QueueState<T> = when (result) {
    is AppResult.Success -> QueueState(items = result.data, loaded = true)
    // Keep what is already on screen; the error only replaces the list when there is nothing to show.
    is AppResult.Failure -> copy(isLoading = false, error = result.error.toUiText())
}

private fun ModerationUiState.withoutItem(d: PendingDecision) = when (d.queue) {
    ReviewQueue.STORIES -> copy(stories = stories.copy(items = stories.items.filterNot { it.detail.story.id == d.id }))
    ReviewQueue.PROJECTS -> copy(projects = projects.copy(items = projects.items.filterNot { it.project.id == d.id }))
}
