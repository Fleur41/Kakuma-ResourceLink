package com.kakuma.resourcelink.feature.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.map
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.IconTile
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrTextButton
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.notifications.domain.AppNotification
import com.kakuma.resourcelink.feature.notifications.domain.NotificationRepository
import com.kakuma.resourcelink.feature.notifications.domain.NotificationTarget
import com.kakuma.resourcelink.feature.notifications.domain.NotificationType
import com.kakuma.resourcelink.feature.opportunities.presentation.toPagingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repository: NotificationRepository) : ViewModel() {
    /** Ids read during this session, so the list updates without a reload. */
    private val readLocally = MutableStateFlow(emptySet<String>())
    private val allRead = MutableStateFlow(false)
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    val notifications: Flow<PagingData<AppNotification>> = combine(
        repository.notifications().cachedIn(viewModelScope), readLocally, allRead,
    ) { page, read, all -> page.map { n -> if (all || n.id in read) n.copy(isRead = true) else n } }

    fun open(n: AppNotification, navigate: (NotificationTarget) -> Unit) {
        if (!n.isRead) viewModelScope.launch {
            if (repository.markRead(n.id) is AppResult.Success) readLocally.value = readLocally.value + n.id
        }
        navigate(n.target)
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (val r = repository.markAllRead()) {
                is AppResult.Success -> { allRead.value = true; uiEvents.toast(UiText.Res(R.string.toast_all_read)) }
                is AppResult.Failure -> uiEvents.snackbar(r.error.toUiText())
            }
        }
    }
}

@Composable
fun NotificationsRoute(
    onBack: () -> Unit,
    onNavigate: (NotificationTarget) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val items = viewModel.notifications.collectAsLazyPagingItems()
    UiEventEffect(viewModel.events)
    Column(Modifier.fillMaxSize()) {
        KrTopBar(stringResource(R.string.notifications_title), onBack = onBack, actions = {
            KrTextButton(stringResource(R.string.action_mark_all_read), viewModel::markAllRead)
        })
        val refresh = items.loadState.refresh
        when {
            refresh is LoadState.Loading && items.itemCount == 0 -> SkeletonCardList()
            refresh is LoadState.Error && items.itemCount == 0 -> ErrorState(refresh.error.toPagingMessage().asString(), { items.retry() })
            refresh is LoadState.NotLoading && items.itemCount == 0 -> EmptyState(
                Icons.Outlined.NotificationsNone, stringResource(R.string.notifications_empty_title), stringResource(R.string.notifications_empty_body),
            )
            else -> LazyColumn(contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                items(items.itemCount, key = items.itemKey { it.id }) { i ->
                    items[i]?.let { n -> NotificationRow(n) { viewModel.open(n, onNavigate) } }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    val unreadLabel = stringResource(R.string.cd_unread)
    KrCard(onClick = onClick, modifier = Modifier.semantics { if (!n.isRead) stateDescription = unreadLabel }) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(
                when (n.type) {
                    NotificationType.MENTORSHIP_REQUEST, NotificationType.MENTORSHIP_UPDATE -> Icons.Outlined.Handshake
                    NotificationType.STORY_PUBLISHED, NotificationType.STORY_REJECTED, NotificationType.STORY_COMMENT -> Icons.Outlined.AutoStories
                    NotificationType.PROJECT_CONTRIBUTION, NotificationType.PROJECT_STATUS -> Icons.Outlined.Groups
                    NotificationType.SYSTEM -> Icons.Outlined.Notifications
                },
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(n.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (n.isRead) FontWeight.Medium else FontWeight.Bold))
                if (n.body.isNotBlank()) Text(n.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!n.isRead) Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
        }
    }
}
