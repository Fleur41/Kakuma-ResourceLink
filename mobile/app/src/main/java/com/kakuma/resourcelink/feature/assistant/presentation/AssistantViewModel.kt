package com.kakuma.resourcelink.feature.assistant.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.connectivity.ConnectivityObserver
import com.kakuma.resourcelink.core.locale.LanguageManager
import com.kakuma.resourcelink.feature.assistant.domain.AssistantRepository
import com.kakuma.resourcelink.feature.assistant.domain.AssistantTask
import com.kakuma.resourcelink.feature.assistant.domain.ChatMessage
import com.kakuma.resourcelink.feature.assistant.domain.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isThinking: Boolean = false,
    /** Shown inline under the conversation; the last user message can be retried. */
    val error: UiText? = null,
    val canRetry: Boolean = false,
    val isOnline: Boolean = true,
    val isLoadingConversation: Boolean = false,
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: AssistantRepository,
    private val languageManager: LanguageManager,
    connectivity: ConnectivityObserver,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = combine(_state, connectivity.isOnline) { s, online -> s.copy(isOnline = online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssistantUiState())

    private var conversationId: String?
        get() = savedState["conversationId"]
        set(value) { savedState["conversationId"] = value }
    private var lastRequest: (suspend () -> Unit)? = null
    /** The reply or conversation load in progress; cancelled when the user switches chats so it can't land in the new one. */
    private var inFlight: Job? = null

    init {
        val task = AssistantTask.from(savedState["task"])
        val targetId: String? = savedState["targetId"]
        when {
            // Restored after process death: reload the conversation from the server.
            conversationId != null -> loadConversation(conversationId!!)
            task != AssistantTask.CHAT && targetId != null && savedState.get<Boolean>("taskStarted") != true -> {
                savedState["taskStarted"] = true
                startTask(task, targetId)
            }
        }
    }

    fun onInput(v: String) = _state.update { it.copy(input = v.take(4000)) }

    fun send(text: String = _state.value.input) {
        val message = text.trim()
        if (message.isEmpty() || _state.value.isThinking) return
        _state.update { it.copy(input = "", messages = it.messages + ChatMessage(UUID.randomUUID().toString(), true, message)) }
        request { repository.send(message, conversationId, languageManager.current().tag) }
    }

    /** Explain/summarize: the backend builds the grounded prompt from [task] + [targetId]; the reply uses the app language. */
    private fun startTask(task: AssistantTask, targetId: String) {
        val summarize = task == AssistantTask.SUMMARIZE_STORY
        _state.update { it.copy(messages = listOf(ChatMessage(if (summarize) TASK_SUMMARIZE else TASK_EXPLAIN, true, ""))) }
        val instruction = if (summarize) "Summarize this story." else "Explain this opportunity."
        request { repository.send(instruction, null, languageManager.current().tag, task, targetId) }
    }

    private fun request(call: suspend () -> AppResult<com.kakuma.resourcelink.feature.assistant.domain.ChatReply>) {
        lastRequest = {
            _state.update { it.copy(isThinking = true, error = null, canRetry = false) }
            when (val r = call()) {
                is AppResult.Success -> {
                    conversationId = r.data.conversationId
                    _state.update { it.copy(isThinking = false, messages = it.messages + r.data.message) }
                }
                is AppResult.Failure -> _state.update {
                    it.copy(
                        isThinking = false,
                        error = if (r.error is AppError.Unavailable) UiText.Res(R.string.ai_unavailable) else r.error.toUiText(),
                        canRetry = r.error == AppError.Network || r.error is AppError.Server || r.error is AppError.Unavailable,
                    )
                }
            }
        }
        lastRequest?.let(::launchExclusive)
    }

    fun retry() { lastRequest?.let { launchExclusive(it) } }

    private fun launchExclusive(block: suspend () -> Unit) {
        inFlight?.cancel()
        inFlight = viewModelScope.launch { block() }
    }

    fun newConversation() {
        inFlight?.cancel()
        lastRequest = null
        conversationId = null
        _state.value = AssistantUiState()
    }

    fun loadConversation(id: String) {
        conversationId = id
        lastRequest = null
        _state.update { it.copy(isLoadingConversation = true, isThinking = false, messages = emptyList(), error = null) }
        launchExclusive {
            when (val r = repository.conversation(id)) {
                is AppResult.Success -> _state.update { it.copy(isLoadingConversation = false, messages = r.data) }
                is AppResult.Failure -> _state.update { it.copy(isLoadingConversation = false, error = r.error.toUiText()) }
            }
        }
    }

    companion object {
        /** Placeholder ids for the user bubble of a one-tap task (rendered with localized text). */
        const val TASK_EXPLAIN = "task:explain"
        const val TASK_SUMMARIZE = "task:summarize"
    }
}

data class HistoryUiState(val items: List<Conversation> = emptyList(), val isLoading: Boolean = true, val error: UiText? = null)

@HiltViewModel
class AssistantHistoryViewModel @Inject constructor(private val repository: AssistantRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state = _state.asStateFlow()
    private val uiEvents = com.kakuma.resourcelink.core.ui.UiEventChannel()
    val events = uiEvents.events

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            _state.value = when (val r = repository.conversations()) {
                is AppResult.Success -> HistoryUiState(r.data, false)
                is AppResult.Failure -> HistoryUiState(isLoading = false, error = r.error.toUiText())
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val r = repository.delete(id)) {
                is AppResult.Success -> {
                    _state.update { it.copy(items = it.items.filterNot { c -> c.id == id }) }
                    uiEvents.toast(UiText.Res(R.string.toast_conversation_deleted))
                }
                is AppResult.Failure -> uiEvents.snackbar(r.error.toUiText())
            }
        }
    }
}
