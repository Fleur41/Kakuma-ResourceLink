package com.kakuma.resourcelink.feature.stories.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.BadgeTone
import com.kakuma.resourcelink.core.designsystem.component.CategoryPill
import com.kakuma.resourcelink.core.designsystem.component.ConfirmDialog
import com.kakuma.resourcelink.core.designsystem.component.CoverImage
import com.kakuma.resourcelink.core.designsystem.component.FullCoverImage
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.FullScreenLoading
import com.kakuma.resourcelink.core.designsystem.component.InitialAvatar
import com.kakuma.resourcelink.core.designsystem.component.KrOutlinedButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.designsystem.component.StatusBadge
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.stories.domain.StoryStatus

@Composable
fun StoryDetailRoute(
    onBack: () -> Unit,
    onSummarize: (String) -> Unit,
    onRequireLogin: () -> Unit,
    viewModel: StoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    UiEventEffect(viewModel.events)
    val detail = state.detail

    Scaffold(topBar = {
        KrTopBar("", onBack = onBack, actions = {
            if (detail != null && detail.story.status == StoryStatus.PUBLISHED) {
                val shareTitle = stringResource(R.string.share_story_chooser)
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, detail.story.title)
                        putExtra(Intent.EXTRA_TEXT, "${detail.story.title}\n\n${detail.story.excerpt}")
                    }
                    context.startActivity(Intent.createChooser(send, shareTitle))
                }) { Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.cd_share)) }
            }
            if (state.isOwner) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                }
            }
        })
    }) { padding ->
        when {
            detail == null && state.isLoading -> FullScreenLoading(Modifier.padding(padding))
            detail == null -> ErrorState(state.error?.asString().orEmpty(), viewModel::refresh, Modifier.padding(padding))
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = Spacing.gutter)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                val story = detail.story
                // The whole photo as uploaded; only stories without a photo get the decorative banner.
                val cover = story.coverImage
                if (!cover.isNullOrBlank()) FullCoverImage(cover)
                else CoverImage(null, Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.large), Icons.Outlined.AutoStories)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPill(story.category.label())
                    if (story.status != StoryStatus.PUBLISHED) StoryStatusBadge(story.status)
                    if (detail.isOfflineCopy) StatusBadge(stringResource(R.string.badge_offline_copy), BadgeTone.Info)
                }
                Text(story.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialAvatar(story.author.name, story.author.avatarUrl, size = 40.dp)
                    Spacer(Modifier.width(Spacing.xs))
                    Column(Modifier.weight(1f)) {
                        Text(story.author.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            listOfNotNull(story.author.country?.ifBlank { null }, androidx.compose.ui.res.pluralStringResource(R.plurals.reading_minutes, story.readingTime, story.readingTime)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (story.status == StoryStatus.REJECTED && detail.rejectionReason.isNotBlank()) {
                    Text(
                        stringResource(R.string.story_rejection_reason, detail.rejectionReason),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
                    )
                }
                if (story.status == StoryStatus.PENDING_REVIEW) {
                    Text(stringResource(R.string.story_pending_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                MarkdownText(detail.body.ifBlank { story.excerpt })
                if (story.status == StoryStatus.PUBLISHED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoryActions(
                            story,
                            onToggleLike = { if (state.isLoggedIn) viewModel.toggleLike() else onRequireLogin() },
                            onToggleBookmark = { if (state.isLoggedIn) viewModel.toggleBookmark() else onRequireLogin() },
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Outlined.OfflinePin, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.story_saved_offline), style = MaterialTheme.typography.bodySmall)
                    }
                    KrOutlinedButton(
                        stringResource(R.string.action_summarize_ai), onClick = { onSummarize(story.id) },
                        icon = Icons.Outlined.AutoAwesome, modifier = Modifier.fillMaxWidth(),
                    )
                    CommentsSection(
                        state = comments,
                        isLoggedIn = state.isLoggedIn,
                        onChange = viewModel::onCommentChange,
                        onSend = { if (state.isLoggedIn) viewModel.sendComment() else onRequireLogin() },
                        onRetry = viewModel::loadComments,
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_story_title),
            message = stringResource(R.string.dialog_delete_story_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; viewModel.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun CommentsSection(
    state: CommentsUiState,
    isLoggedIn: Boolean,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            stringResource(R.string.comments_title, state.comments.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.error != null -> ErrorState(state.error.asString(), onRetry)
            state.comments.isEmpty() -> Text(stringResource(R.string.comments_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.comments.forEach { c ->
                Row {
                    InitialAvatar(c.author.name, c.author.avatarUrl, size = 32.dp)
                    Spacer(Modifier.width(Spacing.xs))
                    Column {
                        Text(c.author.name, style = MaterialTheme.typography.labelLarge)
                        Text(c.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            KrTextField(
                value = state.draft,
                onValueChange = onChange,
                label = stringResource(R.string.comments_add),
                placeholder = stringResource(if (isLoggedIn) R.string.comments_placeholder else R.string.comments_sign_in),
                singleLine = false,
                imeAction = ImeAction.Default,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.isSending) {
                if (state.isSending) CircularProgressIndicator(Modifier.padding(8.dp), strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.cd_send), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
