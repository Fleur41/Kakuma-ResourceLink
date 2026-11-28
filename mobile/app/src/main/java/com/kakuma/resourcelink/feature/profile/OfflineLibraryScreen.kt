package com.kakuma.resourcelink.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlexDirection.Companion.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.stories.domain.StoryRepository
import com.kakuma.resourcelink.feature.stories.presentation.label
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineLibraryViewModel @Inject constructor(private val stories: StoryRepository) : ViewModel() {
    val articles = stories.offlineArticles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    fun remove(id: String) = viewModelScope.launch {
        stories.removeOffline(id)
        uiEvents.toast(UiText.Res(R.string.toast_removed_offline))
    }
}

@Composable
fun OfflineLibraryRoute(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: OfflineLibraryViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    UiEventEffect(viewModel.events)
    Scaffold(topBar = { KrTopBar(stringResource(R.string.settings_offline_library), onBack = onBack) }) { padding ->
        val list = articles
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(
                Icons.Outlined.DownloadForOffline, stringResource(R.string.offline_empty_title), stringResource(R.string.offline_empty_body), Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                item { Text(stringResource(R.string.offline_library_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(list, key = { it.id }) { s ->
                    KrCard(onClick = { onOpen(s.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.title, style = MaterialTheme.typography.titleSmall)
                                Text("${s.category.label()} · ${s.author.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.remove(s.id) }) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.action_remove_offline)) }
                        }
                    }
                }
            }
        }
    }
}
