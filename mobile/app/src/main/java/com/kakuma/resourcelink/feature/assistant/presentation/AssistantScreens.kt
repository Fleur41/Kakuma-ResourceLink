package com.kakuma.resourcelink.feature.assistant.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.ConfirmDialog
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.FullScreenLoading
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.designsystem.component.OfflineBanner
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.assistant.domain.ChatMessage
import com.kakuma.resourcelink.feature.assistant.domain.Source
import com.kakuma.resourcelink.feature.stories.presentation.MarkdownText

@Composable
fun AssistantRoute(
    onBack: (() -> Unit)?,
    onOpenSource: (Source) -> Unit,
    onHistory: () -> Unit,
    conversationToOpen: String?,
    onConversationOpened: () -> Unit,
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(conversationToOpen) {
        conversationToOpen?.let { viewModel.loadConversation(it); onConversationOpened() }
    }
    LaunchedEffect(state.messages.size, state.isThinking) {
        val count = state.messages.size + if (state.isThinking) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    Scaffold(topBar = {
        KrTopBar(stringResource(R.string.assistant_title), onBack = onBack, actions = {
            IconButton(onClick = viewModel::newConversation, enabled = state.messages.isNotEmpty() || state.isThinking) { Icon(Icons.Outlined.AddComment, stringResource(R.string.cd_new_chat)) }
            IconButton(onClick = onHistory) { Icon(Icons.Outlined.History, stringResource(R.string.assistant_history)) }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (!state.isOnline) OfflineBanner()
            Box(Modifier.weight(1f)) {
                when {
                    state.isLoadingConversation -> FullScreenLoading()
                    state.messages.isEmpty() -> Welcome(enabled = state.isOnline, onPrompt = viewModel::send)
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(Spacing.gutter),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(state.messages, key = { it.id }) { Bubble(it, onOpenSource) }
                        if (state.isThinking) item(key = "thinking") { Thinking() }
                        state.error?.let { err ->
                            item(key = "error") { ErrorState(err.asString(), onRetry = viewModel::retry) }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.assistant_disclaimer), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.gutter),
            )
            Row(Modifier.fillMaxWidth().padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(if (state.isOnline) R.string.assistant_input_hint else R.string.ai_requires_internet)) },
                    enabled = state.isOnline,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.send() }),
                )
                Spacer(Modifier.size(8.dp))
                FilledIconButton(onClick = { viewModel.send() }, enabled = state.isOnline && state.input.isNotBlank() && !state.isThinking) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.cd_send))
                }
            }
        }
    }
}

@Composable
private fun Welcome(enabled: Boolean, onPrompt: (String) -> Unit) {
    val prompts = listOf(
        stringResource(R.string.ai_prompt_scholarships),
        stringResource(R.string.ai_prompt_cv),
        stringResource(R.string.ai_prompt_health),
        stringResource(R.string.ai_prompt_mentor),
    )
    Column(
        Modifier.fillMaxSize().padding(Spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
        Text(stringResource(R.string.assistant_welcome_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = Spacing.sm))
        Text(
            stringResource(R.string.assistant_welcome_body), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = Spacing.md),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            prompts.forEach { p -> SuggestionChip(onClick = { onPrompt(p) }, label = { Text(p) }, enabled = enabled) }
        }
    }
}

@Composable
private fun Bubble(message: ChatMessage, onOpenSource: (Source) -> Unit) {
    val text = when (message.id) {
        AssistantViewModel.TASK_EXPLAIN -> stringResource(R.string.ai_task_explain)
        AssistantViewModel.TASK_SUMMARIZE -> stringResource(R.string.ai_task_summarize)
        else -> message.content
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, if (message.fromUser) 4.dp else 18.dp, if (message.fromUser) 18.dp else 4.dp))
                .background(if (message.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .padding(12.dp),
        ) {
            if (message.fromUser) Text(text, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyMedium)
            else MarkdownText(text)
            if (message.sources.isNotEmpty()) {
                Text(
                    stringResource(R.string.assistant_sources), style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.sources.forEach { s ->
                        AssistChip(onClick = { onOpenSource(s) }, label = { Text(s.title, maxLines = 1) }, leadingIcon = { Icon(Icons.Outlined.Link, null) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Thinking() {
    Row(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(stringResource(R.string.assistant_thinking), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun AssistantHistoryRoute(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: AssistantHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    UiEventEffect(viewModel.events)
    Scaffold(topBar = { KrTopBar(stringResource(R.string.assistant_history), onBack = onBack) }) { padding ->
        when {
            state.isLoading -> FullScreenLoading(Modifier.padding(padding))
            state.error != null -> ErrorState(state.error!!.asString(), viewModel::load, Modifier.padding(padding))
            state.items.isEmpty() -> EmptyState(
                Icons.Outlined.History, stringResource(R.string.assistant_history_empty_title),
                stringResource(R.string.assistant_history_empty_body), Modifier.padding(padding),
            )
            else -> LazyColumn(
                Modifier.padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.items, key = { it.id }) { c ->
                    KrCard(onClick = { onOpen(c.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.title.ifBlank { stringResource(R.string.assistant_untitled) }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { confirm = c.id }) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.action_delete)) }
                        }
                    }
                }
            }
        }
    }
    confirm?.let { id ->
        ConfirmDialog(
            stringResource(R.string.dialog_delete_chat_title), stringResource(R.string.dialog_delete_chat_body), stringResource(R.string.action_delete),
            onConfirm = { confirm = null; viewModel.delete(id) }, onDismiss = { confirm = null },
        )
    }
}
