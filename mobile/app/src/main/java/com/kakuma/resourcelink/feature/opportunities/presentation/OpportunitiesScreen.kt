package com.kakuma.resourcelink.feature.opportunities.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.KrFilterChips
import com.kakuma.resourcelink.core.designsystem.component.KrSearchField
import com.kakuma.resourcelink.core.designsystem.component.OfflineBanner
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityCategory
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.toUiText
import java.io.IOException

/** Maps paging errors to user-facing text without exposing exception details. */
fun Throwable.toPagingMessage(): UiText =
    if (this is IOException) AppError.Network.toUiText() else AppError.Server(null).toUiText()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitiesRoute(onOpen: (String) -> Unit, onBack: (() -> Unit)? = null, viewModel: OpportunitiesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val items = viewModel.opportunities.collectAsLazyPagingItems()
    UiEventEffect(viewModel.events)

    Column(Modifier.fillMaxSize()) {
        val title = stringResource(if (state.bookmarkedOnly) R.string.opp_saved_title else R.string.opp_title)
        if (onBack != null) com.kakuma.resourcelink.core.designsystem.component.KrTopBar(title, onBack = onBack)
        if (!online) OfflineBanner()
        if (onBack == null) Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.sm).semantics { heading() },
        )
        if (!state.bookmarkedOnly) {
            KrSearchField(
                state.query, viewModel::onQueryChange, stringResource(R.string.opp_search_hint),
                Modifier.padding(horizontal = Spacing.gutter),
            )
            val options = listOf<Pair<OpportunityCategory?, String>>(null to stringResource(R.string.filter_all)) +
                OpportunityCategory.entries.map { it to it.label() }
            KrFilterChips(options, state.category, viewModel::onCategoryChange, Modifier.padding(vertical = Spacing.xs))
        }

        val refresh = items.loadState.refresh
        val searchingInCategory = !state.bookmarkedOnly && state.category != null && state.query.isNotBlank()
        PullToRefreshBox(
            isRefreshing = refresh is LoadState.Loading && items.itemCount > 0,
            onRefresh = { items.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                refresh is LoadState.Loading && items.itemCount == 0 -> SkeletonCardList()
                refresh is LoadState.Error && items.itemCount == 0 -> ErrorState(
                    refresh.error.toPagingMessage().asString(), onRetry = { items.retry() },
                )
                refresh is LoadState.NotLoading && items.itemCount == 0 -> EmptyState(
                    Icons.Outlined.SearchOff,
                    stringResource(if (state.bookmarkedOnly) R.string.opp_saved_empty_title else R.string.opp_empty_title),
                    stringResource(if (state.bookmarkedOnly) R.string.opp_saved_empty_body else R.string.opp_empty_body),
                    // A search with a category chip on only looks in that category: offer to widen it.
                    actionLabel = if (searchingInCategory) stringResource(R.string.opp_search_all) else null,
                    onAction = if (searchingInCategory) ({ viewModel.onCategoryChange(null) }) else null,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(items.itemCount, key = items.itemKey { it.id }) { index ->
                        items[index]?.let { opp ->
                            OpportunityCard(opp, onClick = { onOpen(opp.id) }, onToggleBookmark = { viewModel.onToggleBookmark(opp) })
                        }
                    }
                    when (val append = items.loadState.append) {
                        is LoadState.Loading -> item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        }
                        is LoadState.Error -> item {
                            ErrorState(append.error.toPagingMessage().asString(), onRetry = { items.retry() })
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
