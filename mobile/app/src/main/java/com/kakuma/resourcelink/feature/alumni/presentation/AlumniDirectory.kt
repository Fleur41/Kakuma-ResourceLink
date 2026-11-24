package com.kakuma.resourcelink.feature.alumni.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.LoadState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.BadgeTone
import com.kakuma.resourcelink.core.designsystem.component.CategoryPill
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.InitialAvatar
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrSearchField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.designsystem.component.StatusBadge
import com.kakuma.resourcelink.feature.alumni.domain.AlumniProfile
import com.kakuma.resourcelink.feature.alumni.domain.AlumniQuery
import com.kakuma.resourcelink.feature.alumni.domain.AlumniRepository
import com.kakuma.resourcelink.feature.opportunities.presentation.toPagingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AlumniDirectoryViewModel @Inject constructor(
    repository: AlumniRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    val search = savedState.getStateFlow("q", "")
    val mentorsOnly = savedState.getStateFlow("mentors", savedState.get<Boolean>("mentorsOnly") ?: false)

    val alumni: Flow<PagingData<AlumniProfile>> = combine(search.debounce(300), mentorsOnly, ::AlumniQuery)
        .distinctUntilChanged()
        .flatMapLatest { repository.directory(it) }
        .cachedIn(viewModelScope)

    fun onSearch(q: String) { savedState["q"] = q }
    fun toggleMentors() { savedState["mentors"] = !mentorsOnly.value }
}

@Composable
fun AlumniDirectoryRoute(onBack: (() -> Unit)?, onOpen: (String) -> Unit, onEditMyProfile: () -> Unit, viewModel: AlumniDirectoryViewModel = hiltViewModel()) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val mentorsOnly by viewModel.mentorsOnly.collectAsStateWithLifecycle()
    val items = viewModel.alumni.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        KrTopBar(stringResource(R.string.alumni_title), onBack = onBack, actions = {
            com.kakuma.resourcelink.core.designsystem.component.KrTextButton(stringResource(R.string.alumni_my_profile), onEditMyProfile)
        })
        Text(
            stringResource(R.string.alumni_subtitle), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.gutter),
        )
        KrSearchField(search, viewModel::onSearch, stringResource(R.string.alumni_search_hint), Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.xs))
        Row(Modifier.padding(horizontal = Spacing.gutter)) {
            FilterChip(selected = mentorsOnly, onClick = viewModel::toggleMentors, label = { Text(stringResource(R.string.alumni_mentors_only)) })
        }
        val refresh = items.loadState.refresh
        when {
            refresh is LoadState.Loading && items.itemCount == 0 -> SkeletonCardList()
            refresh is LoadState.Error && items.itemCount == 0 -> ErrorState(refresh.error.toPagingMessage().asString(), { items.retry() })
            refresh is LoadState.NotLoading && items.itemCount == 0 -> EmptyState(
                Icons.Outlined.PersonSearch, stringResource(R.string.alumni_empty_title), stringResource(R.string.alumni_empty_body),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(Spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(items.itemCount, key = items.itemKey { it.id }) { i -> items[i]?.let { AlumniCard(it) { onOpen(it.id) } } }
                if (items.loadState.append is LoadState.Loading) item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
fun AlumniCard(profile: AlumniProfile, onClick: () -> Unit) {
    KrCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(profile.fullName, profile.avatarUrl, size = 52.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(profile.fullName, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOf(profile.profession, profile.organization).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(profile.city?.ifBlank { null }, profile.country.ifBlank { null }).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (profile.isMentor) StatusBadge(stringResource(R.string.badge_mentor), BadgeTone.Success)
        }
        if (profile.skills.isNotEmpty()) {
            FlowRow(Modifier.padding(top = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                profile.skills.take(4).forEach { CategoryPill(it) }
            }
        }
    }
}
