package com.kakuma.resourcelink.feature.moderation.presentation

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.CategoryPill
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.FullCoverImage
import com.kakuma.resourcelink.core.designsystem.component.InitialAvatar
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrOutlinedButton
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.moderation.domain.ModerationRules
import com.kakuma.resourcelink.feature.moderation.domain.ReviewProject
import com.kakuma.resourcelink.feature.moderation.domain.ReviewQueue
import com.kakuma.resourcelink.feature.moderation.domain.ReviewStory
import com.kakuma.resourcelink.feature.projects.presentation.label
import com.kakuma.resourcelink.feature.stories.domain.Author
import com.kakuma.resourcelink.feature.stories.presentation.MarkdownText
import com.kakuma.resourcelink.feature.stories.presentation.label
import java.time.OffsetDateTime

/** Review queue for moderators and admins: approve or send back pending stories and community projects. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationRoute(onBack: () -> Unit, viewModel: ModerationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UiEventEffect(viewModel.events)

    Column(Modifier.fillMaxSize()) {
        KrTopBar(stringResource(R.string.moderation_title), onBack = onBack)
        Text(
            stringResource(R.string.moderation_subtitle), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.gutter),
        )
        PrimaryTabRow(selectedTabIndex = state.queue.ordinal, containerColor = MaterialTheme.colorScheme.background) {
            Tab(state.queue == ReviewQueue.STORIES, { viewModel.selectQueue(ReviewQueue.STORIES) }, text = { Text(stringResource(R.string.moderation_tab_stories)) })
            Tab(state.queue == ReviewQueue.PROJECTS, { viewModel.selectQueue(ReviewQueue.PROJECTS) }, text = { Text(stringResource(R.string.moderation_tab_projects)) })
        }
        when (state.queue) {
            ReviewQueue.STORIES -> Queue(state.stories, viewModel::refresh, key = { it.detail.story.id }) { item ->
                StoryReviewCard(item, onDecide = { approve -> viewModel.openDecision(ReviewQueue.STORIES, item.detail.story.id, item.detail.story.title, approve) })
            }
            ReviewQueue.PROJECTS -> Queue(state.projects, viewModel::refresh, key = { it.project.id }) { item ->
                ProjectReviewCard(item, onDecide = { approve -> viewModel.openDecision(ReviewQueue.PROJECTS, item.project.id, item.project.title, approve) })
            }
        }
    }

    state.decision?.let { d ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDecision,
            title = { Text(stringResource(if (d.approve) R.string.moderation_approve_title else R.string.moderation_reject_title, d.title)) },
            text = {
                if (d.takesNote) {
                    KrTextField(
                        state.note, viewModel::onNote,
                        label = stringResource(if (d.needsReason) R.string.moderation_reason else R.string.moderation_note),
                        placeholder = stringResource(R.string.moderation_reason_hint),
                        error = state.noteError?.asString(),
                        singleLine = false, minLines = 3, maxLength = ModerationRules.MAX_NOTE,
                    )
                } else Text(stringResource(R.string.moderation_approve_body))
            },
            confirmButton = {
                KrPrimaryButton(
                    stringResource(if (d.approve) R.string.action_approve else R.string.action_reject),
                    viewModel::confirmDecision, loading = state.isDeciding,
                )
            },
            dismissButton = { TextButton(viewModel::dismissDecision, enabled = !state.isDeciding) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Queue(state: QueueState<T>, onRefresh: () -> Unit, key: (T) -> String, card: @Composable (T) -> Unit) {
    PullToRefreshBox(isRefreshing = state.isLoading && state.loaded, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        when {
            !state.loaded && state.error != null -> ErrorState(state.error.asString(), onRefresh)
            !state.loaded -> SkeletonCardList(count = 2, modifier = Modifier.padding(top = Spacing.sm))
            state.items.isEmpty() -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Scrollable so pull-to-refresh still works on the empty state.
                EmptyState(Icons.Outlined.VerifiedUser, stringResource(R.string.moderation_empty_title), stringResource(R.string.moderation_empty_body))
            }
            else -> LazyColumn(contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(state.items, key = key) { card(it) }
            }
        }
    }
}

@Composable
private fun StoryReviewCard(item: ReviewStory, onDecide: (approve: Boolean) -> Unit) {
    val story = item.detail.story
    var expanded by rememberSaveable(story.id) { mutableStateOf(false) }
    KrCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SubmittedBy(story.author, item.submittedAt)
            CategoryPill(story.category.label())
            Text(story.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (expanded) {
                story.coverImage?.let { FullCoverImage(it, Modifier.fillMaxWidth(), maxHeight = 320.dp) }
                HorizontalDivider()
                MarkdownText(item.detail.body)
            } else if (story.excerpt.isNotBlank()) {
                Text(story.excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            KrTextButton(stringResource(if (expanded) R.string.moderation_hide_story else R.string.moderation_read_story), { expanded = !expanded })
            DecisionButtons(onDecide)
        }
    }
}

@Composable
private fun ProjectReviewCard(item: ReviewProject, onDecide: (approve: Boolean) -> Unit) {
    val p = item.project
    var expanded by rememberSaveable(p.id) { mutableStateOf(false) }
    KrCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SubmittedBy(p.owner, item.submittedAt)
            CategoryPill(p.category.label())
            Text(p.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (p.location.isNotBlank()) Text(p.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(p.summary, style = MaterialTheme.typography.titleSmall)
            if (p.description.isNotBlank()) {
                Text(
                    p.description, style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                )
                KrTextButton(stringResource(if (expanded) R.string.action_show_less else R.string.action_show_more), { expanded = !expanded })
            }
            DecisionButtons(onDecide)
        }
    }
}

@Composable
private fun SubmittedBy(author: Author, submittedAt: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        InitialAvatar(author.name, author.avatarUrl, size = 36.dp)
        Spacer(Modifier.width(Spacing.sm))
        val ago = remember(submittedAt) { submittedAt?.let(::relativeTime) }
        Text(
            if (ago == null) author.name else stringResource(R.string.moderation_submitted_by, author.name, ago),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DecisionButtons(onDecide: (approve: Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(top = Spacing.xs)) {
        KrPrimaryButton(stringResource(R.string.action_approve), { onDecide(true) }, Modifier.weight(1f), icon = Icons.Outlined.Check)
        KrOutlinedButton(stringResource(R.string.action_reject), { onDecide(false) }, Modifier.weight(1f), icon = Icons.Outlined.Close)
    }
}

/** "5 minutes ago", "2 days ago"… in the app's language; null if the timestamp can't be read. */
private fun relativeTime(iso: String): String? = runCatching {
    DateUtils.getRelativeTimeSpanString(
        OffsetDateTime.parse(iso).toInstant().toEpochMilli(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}.getOrNull()
