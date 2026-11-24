package com.kakuma.resourcelink.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.KrColors
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.HeroBanner
import com.kakuma.resourcelink.core.designsystem.component.KrTextButton
import com.kakuma.resourcelink.core.designsystem.component.OfflineBanner
import com.kakuma.resourcelink.core.designsystem.component.QuickAccessTile
import com.kakuma.resourcelink.core.designsystem.component.SectionHeader
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.opportunities.presentation.OpportunityCard
import com.kakuma.resourcelink.feature.stories.presentation.StoryCard

/** Home navigation callbacks (kept in one object so the route signature stays readable). */
data class HomeActions(
    val openSearch: () -> Unit,
    val openOpportunities: () -> Unit,
    val openOpportunity: (String) -> Unit,
    val openServices: () -> Unit,
    val openStories: () -> Unit,
    val openStory: (String) -> Unit,
    val openMentorship: () -> Unit,
    val openAlumni: () -> Unit,
    val openProjects: () -> Unit,
    val openAssistant: () -> Unit,
    val openNotifications: () -> Unit,
    val requireLogin: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UiEventEffect(viewModel.events)
    PullToRefreshBox(isRefreshing = state.isRefreshing && !state.isEmpty, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (!state.isOnline) item { OfflineBanner() }
            item {
                Row(Modifier.fillMaxWidth().padding(start = Spacing.gutter, end = 8.dp, top = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.user?.let { stringResource(R.string.home_greeting, it.firstName) } ?: stringResource(R.string.home_greeting_guest),
                            style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() },
                        )
                        Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val bellLabel = if (state.unread > 0) pluralStringResource(R.plurals.cd_notifications_unread, state.unread, state.unread)
                    else stringResource(R.string.notifications_title)
                    IconButton(onClick = { if (state.user != null) actions.openNotifications() else actions.requireLogin() }) {
                        BadgedBox(badge = { if (state.unread > 0) Badge { Text(if (state.unread > 99) "99+" else state.unread.toString()) } }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = bellLabel)
                        }
                    }
                }
            }
            item {
                // Tappable search entry (opens the full opportunity search).
                OutlinedCard(onClick = actions.openSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.home_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
            item {
                HeroBanner(
                    eyebrow = stringResource(R.string.home_hero_eyebrow),
                    title = stringResource(R.string.home_hero_title),
                    actionLabel = stringResource(R.string.home_hero_action),
                    onAction = actions.openOpportunities,
                    icon = Icons.Outlined.School,
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
            }
            item {
                Column(Modifier.padding(horizontal = Spacing.gutter)) {
                    SectionHeader(stringResource(R.string.home_quick_access))
                    val tiles = listOf(
                        Triple(Icons.Outlined.School, R.string.nav_opportunities, actions.openOpportunities),
                        Triple(Icons.Outlined.LocalHospital, R.string.nav_services, actions.openServices),
                        Triple(Icons.AutoMirrored.Outlined.MenuBook, R.string.nav_stories, actions.openStories),
                        Triple(Icons.Outlined.Handshake, R.string.nav_mentorship, actions.openMentorship),
                        Triple(Icons.Outlined.Public, R.string.nav_alumni, actions.openAlumni),
                        Triple(Icons.Outlined.Groups, R.string.nav_projects, actions.openProjects),
                        Triple(Icons.Outlined.AutoAwesome, R.string.nav_assistant, actions.openAssistant),
                    )
                    tiles.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
                            row.forEach { (icon, label, onClick) ->
                                QuickAccessTile(icon, stringResource(label), onClick, Modifier.weight(1f), tint = if (label == R.string.nav_assistant) KrColors.Gold else KrColors.Primary)
                            }
                            repeat(4 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            when {
                state.isEmpty && state.isRefreshing -> item { SkeletonCardList(3) }
                state.isEmpty && state.error != null -> item { ErrorState(state.error!!.asString(), viewModel::refresh) }
                else -> {
                    if (state.opportunities.isNotEmpty()) {
                        item {
                            SectionHeader(stringResource(R.string.home_featured_opportunities), Modifier.padding(horizontal = Spacing.gutter)) {
                                KrTextButton(stringResource(R.string.action_see_all), actions.openOpportunities)
                            }
                        }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = Spacing.gutter), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                items(state.opportunities, key = { it.id }) { o ->
                                    OpportunityCard(
                                        o, onClick = { actions.openOpportunity(o.id) },
                                        onToggleBookmark = { if (state.user != null) viewModel.onToggleBookmark(o) else actions.requireLogin() },
                                        modifier = Modifier.width(300.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (state.stories.isNotEmpty()) {
                        item {
                            SectionHeader(stringResource(R.string.home_latest_stories), Modifier.padding(horizontal = Spacing.gutter)) {
                                KrTextButton(stringResource(R.string.action_see_all), actions.openStories)
                            }
                        }
                        items(state.stories.take(3), key = { "story-${it.id}" }) { s ->
                            StoryCard(
                                s, onClick = { actions.openStory(s.id) }, onToggleLike = { actions.openStory(s.id) },
                                onToggleBookmark = { actions.openStory(s.id) }, modifier = Modifier.padding(horizontal = Spacing.gutter),
                            )
                        }
                    }
                }
            }
        }
    }
}
