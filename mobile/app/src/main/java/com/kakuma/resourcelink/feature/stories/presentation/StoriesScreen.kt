package com.kakuma.resourcelink.feature.stories.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.BadgeTone
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrFilterChips
import com.kakuma.resourcelink.core.designsystem.component.OfflineBanner
import com.kakuma.resourcelink.core.designsystem.component.SectionHeader
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.designsystem.component.StatusBadge
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.opportunities.presentation.toPagingMessage
import com.kakuma.resourcelink.feature.stories.domain.DraftState
import com.kakuma.resourcelink.feature.stories.domain.StoryCategory
import com.kakuma.resourcelink.feature.stories.domain.StoryDraft
import com.kakuma.resourcelink.feature.stories.domain.StoryFeed

@Composable
private fun StoryFeed.label(): String = stringResource(
    when (this) {
        StoryFeed.LATEST -> R.string.story_feed_latest
        StoryFeed.POPULAR -> R.string.story_feed_popular
        StoryFeed.SAVED -> R.string.story_feed_saved
        StoryFeed.MINE -> R.string.story_feed_mine
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesRoute(
    onOpenStory: (String) -> Unit,
    onWrite: (draftId: String?) -> Unit,
    onRequireLogin: () -> Unit,
    openFeed: StoryFeed? = null,
    onFeedOpened: () -> Unit = {},
    viewModel: StoriesViewModel = hiltViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val loggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val drafts by viewModel.localDrafts.collectAsStateWithLifecycle()
    val items = viewModel.stories.collectAsLazyPagingItems()
    UiEventEffect(viewModel.events)
    // A one-off request from another screen (e.g. Profile > My stories) to show a specific feed.
    LaunchedEffect(openFeed) {
        if (openFeed != null) { viewModel.selectFeed(openFeed); onFeedOpened() }
    }

    val personal = filter.feed == StoryFeed.SAVED || filter.feed == StoryFeed.MINE
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (!online) OfflineBanner()
            Text(
                stringResource(R.string.stories_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.sm).semantics { heading() },
            )
            val feeds = StoryFeed.entries
            PrimaryTabRow(selectedTabIndex = feeds.indexOf(filter.feed), containerColor = MaterialTheme.colorScheme.background) {
                feeds.forEach { feed ->
                    Tab(
                        selected = feed == filter.feed,
                        onClick = { if (feed in setOf(StoryFeed.SAVED, StoryFeed.MINE) && !loggedIn) onRequireLogin() else viewModel.selectFeed(feed) },
                        text = { Text(feed.label(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
            if (!personal) {
                val options = listOf<Pair<StoryCategory?, String>>(null to stringResource(R.string.filter_all)) +
                    StoryCategory.entries.map { it to it.label() }
                KrFilterChips(options, filter.category, viewModel::selectCategory, Modifier.padding(vertical = Spacing.xs))
            }
            val refresh = items.loadState.refresh
            PullToRefreshBox(
                isRefreshing = refresh is LoadState.Loading && items.itemCount > 0,
                onRefresh = { items.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                val showDrafts = filter.feed == StoryFeed.MINE && drafts.isNotEmpty()
                when {
                    refresh is LoadState.Loading && items.itemCount == 0 && !showDrafts -> SkeletonCardList()
                    refresh is LoadState.Error && items.itemCount == 0 && !showDrafts ->
                        ErrorState(refresh.error.toPagingMessage().asString(), onRetry = { items.retry() })
                    refresh is LoadState.NotLoading && items.itemCount == 0 && !showDrafts -> EmptyState(
                        if (filter.feed == StoryFeed.MINE) Icons.Outlined.EditNote else Icons.Outlined.AutoStories,
                        stringResource(
                            when (filter.feed) {
                                StoryFeed.MINE -> R.string.stories_mine_empty_title
                                StoryFeed.SAVED -> R.string.stories_saved_empty_title
                                else -> R.string.stories_empty_title
                            },
                        ),
                        stringResource(
                            when (filter.feed) {
                                StoryFeed.MINE -> R.string.stories_mine_empty_body
                                StoryFeed.SAVED -> R.string.stories_saved_empty_body
                                else -> R.string.stories_empty_body
                            },
                        ),
                        actionLabel = if (filter.feed == StoryFeed.MINE) stringResource(R.string.action_write_story) else null,
                        onAction = { onWrite(null) },
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (showDrafts) {
                            item { SectionHeader(stringResource(R.string.stories_on_device)) }
                            drafts.forEach { draft -> item(key = "draft-${draft.localId}") { DraftCard(draft) { onWrite(draft.localId) } } }
                            item { SectionHeader(stringResource(R.string.stories_submitted)) }
                        }
                        items(items.itemCount, key = items.itemKey { it.id }) { index ->
                            items[index]?.let { story ->
                                StoryCard(
                                    story,
                                    onClick = { onOpenStory(story.id) },
                                    onToggleLike = { if (loggedIn) viewModel.toggleLike(story) else onRequireLogin() },
                                    onToggleBookmark = { if (loggedIn) viewModel.toggleBookmark(story) else onRequireLogin() },
                                    showStatus = filter.feed == StoryFeed.MINE,
                                )
                            }
                        }
                        if (items.loadState.append is LoadState.Loading) item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        }
                        (items.loadState.append as? LoadState.Error)?.let { err ->
                            item { ErrorState(err.error.toPagingMessage().asString(), onRetry = { items.retry() }) }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { if (loggedIn) onWrite(null) else onRequireLogin() },
            icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            text = { Text(stringResource(R.string.action_share_story)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
        )
    }
}

@Composable
private fun DraftCard(draft: StoryDraft, onClick: () -> Unit) {
    KrCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (draft.state == DraftState.PENDING_UPLOAD) Icons.Outlined.CloudUpload else Icons.Outlined.EditNote,
                contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    draft.title.ifBlank { stringResource(R.string.story_untitled) },
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(draft.category.label(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (draft.state == DraftState.PENDING_UPLOAD) StatusBadge(stringResource(R.string.draft_waiting_upload), BadgeTone.Info)
            else StatusBadge(stringResource(R.string.draft_on_device), BadgeTone.Neutral)
        }
    }
}
