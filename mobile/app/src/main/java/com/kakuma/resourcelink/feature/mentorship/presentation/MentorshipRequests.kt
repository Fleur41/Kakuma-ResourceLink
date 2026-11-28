package com.kakuma.resourcelink.feature.mentorship.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.BadgeTone
import com.kakuma.resourcelink.core.designsystem.component.ConfirmDialog
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.InitialAvatar
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrOutlinedButton
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.designsystem.component.StatusBadge
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipBox
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipRepository
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipRequest
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipRules
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipStatus
import com.kakuma.resourcelink.feature.opportunities.presentation.toPagingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MentorshipRequestsViewModel @Inject constructor(
    private val repository: MentorshipRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    val box = savedState.getStateFlow("box", savedState.get<String>("initialBox") ?: MentorshipBox.SENT.apiValue)
    /** Bumped after an update so the list reloads with the new status. */
    private val version = MutableStateFlow(0)
    private val _busyId = MutableStateFlow<String?>(null)
    val busyId = _busyId.asStateFlow()
    val requests = combine(box, version) { b, _ -> b }
        .flatMapLatest { b -> repository.requests(MentorshipBox.entries.first { it.apiValue == b }) }
        .cachedIn(viewModelScope)
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    fun selectBox(b: MentorshipBox) { savedState["box"] = b.apiValue }

    fun update(request: MentorshipRequest, status: MentorshipStatus) {
        _busyId.value = request.id
        viewModelScope.launch {
            when (val r = repository.updateStatus(request.id, status, null)) {
                is AppResult.Success -> {
                    uiEvents.toast(
                        UiText.Res(
                            when (status) {
                                MentorshipStatus.ACCEPTED -> R.string.toast_mentorship_accepted
                                MentorshipStatus.DECLINED -> R.string.toast_mentorship_declined
                                MentorshipStatus.CANCELLED -> R.string.toast_mentorship_cancelled
                                else -> R.string.toast_mentorship_completed
                            },
                        ),
                    )
                    version.value++
                }
                is AppResult.Failure -> uiEvents.snackbar(r.error.toUiText())
            }
            _busyId.value = null
        }
    }
}

@Composable
fun MentorshipStatusBadge(status: MentorshipStatus) {
    val (label, tone) = when (status) {
        MentorshipStatus.PENDING -> R.string.mentorship_status_pending to BadgeTone.Warning
        MentorshipStatus.ACCEPTED -> R.string.mentorship_status_accepted to BadgeTone.Success
        MentorshipStatus.DECLINED -> R.string.mentorship_status_declined to BadgeTone.Error
        MentorshipStatus.CANCELLED -> R.string.mentorship_status_cancelled to BadgeTone.Neutral
        MentorshipStatus.COMPLETED -> R.string.mentorship_status_completed to BadgeTone.Info
    }
    StatusBadge(stringResource(label), tone)
}

@Composable
fun MentorshipRequestsRoute(
    onBack: () -> Unit,
    onFindMentor: () -> Unit,
    viewModel: MentorshipRequestsViewModel = hiltViewModel()
) {
    val boxValue by viewModel.box.collectAsStateWithLifecycle()
    val busyId by viewModel.busyId.collectAsStateWithLifecycle()
    val items = viewModel.requests.collectAsLazyPagingItems()
    val box = MentorshipBox.entries.first { it.apiValue == boxValue }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) } // request id awaiting decline/cancel confirmation
    var confirmStatus by rememberSaveable { mutableStateOf<String?>(null) }
    UiEventEffect(viewModel.events)

    Column(Modifier.fillMaxSize()) {
        KrTopBar(stringResource(R.string.mentorship_title), onBack = onBack)
        PrimaryTabRow(selectedTabIndex = box.ordinal, containerColor = MaterialTheme.colorScheme.background) {
            Tab(box == MentorshipBox.SENT, { viewModel.selectBox(MentorshipBox.SENT) }, text = { Text(stringResource(R.string.mentorship_sent)) })
            Tab(box == MentorshipBox.RECEIVED, { viewModel.selectBox(MentorshipBox.RECEIVED) }, text = { Text(stringResource(R.string.mentorship_received)) })
        }
        val refresh = items.loadState.refresh
        when {
            refresh is LoadState.Loading && items.itemCount == 0 -> SkeletonCardList(modifier = Modifier.padding(top = Spacing.sm))
            refresh is LoadState.Error && items.itemCount == 0 -> ErrorState(refresh.error.toPagingMessage().asString(), { items.retry() })
            refresh is LoadState.NotLoading && items.itemCount == 0 -> EmptyState(
                Icons.Outlined.Handshake,
                stringResource(if (box == MentorshipBox.SENT) R.string.mentorship_sent_empty_title else R.string.mentorship_received_empty_title),
                stringResource(if (box == MentorshipBox.SENT) R.string.mentorship_sent_empty_body else R.string.mentorship_received_empty_body),
                actionLabel = if (box == MentorshipBox.SENT) stringResource(R.string.action_find_mentor) else null,
                onAction = onFindMentor,
            )
            else -> LazyColumn(contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(items.itemCount, key = items.itemKey { it.id }) { i ->
                    items[i]?.let { req ->
                        RequestCard(req, box, busy = busyId == req.id) { status ->
                            if (status == MentorshipStatus.DECLINED || status == MentorshipStatus.CANCELLED) {
                                confirm = req.id; confirmStatus = status.apiValue
                            } else viewModel.update(req, status)
                        }
                    }
                }
            }
        }
    }
    val pendingId = confirm
    val pendingStatus = confirmStatus?.let(MentorshipStatus::from)
    if (pendingId != null && pendingStatus != null) {
        val isCancel = pendingStatus == MentorshipStatus.CANCELLED
        ConfirmDialog(
            title = stringResource(if (isCancel) R.string.dialog_cancel_request_title else R.string.dialog_decline_request_title),
            message = stringResource(if (isCancel) R.string.dialog_cancel_request_body else R.string.dialog_decline_request_body),
            confirmLabel = stringResource(if (isCancel) R.string.action_cancel_request else R.string.action_decline),
            onConfirm = {
                val req = items.itemSnapshotList.items.firstOrNull { it.id == pendingId }
                if (req != null) viewModel.update(req, pendingStatus)
                confirm = null; confirmStatus = null
            },
            onDismiss = { confirm = null; confirmStatus = null },
        )
    }
}

@Composable
private fun RequestCard(req: MentorshipRequest, box: MentorshipBox, busy: Boolean, onAction: (MentorshipStatus) -> Unit) {
    val other = if (box == MentorshipBox.SENT) req.mentor else req.mentee
    KrCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(other.name, other.avatarUrl)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(other.name, style = MaterialTheme.typography.titleSmall)
                Text(req.goal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MentorshipStatusBadge(req.status)
        }
        Text(req.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.xs))
        if (req.helpAreas.isNotEmpty()) {
            Text(
                req.helpAreas.map { it.label() }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (req.responseMessage.isNotBlank()) {
            Text(stringResource(R.string.mentorship_reply, req.responseMessage), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        val actions = MentorshipRules.actionsFor(box, req.status)
        if (actions.isNotEmpty()) {
            Row(Modifier.padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { status ->
                    val label = stringResource(
                        when (status) {
                            MentorshipStatus.ACCEPTED -> R.string.action_accept
                            MentorshipStatus.DECLINED -> R.string.action_decline
                            MentorshipStatus.CANCELLED -> R.string.action_cancel_request
                            else -> R.string.action_mark_completed
                        },
                    )
                    if (status == MentorshipStatus.ACCEPTED) KrPrimaryButton(label, { onAction(status) }, loading = busy, modifier = Modifier.weight(1f))
                    else KrOutlinedButton(label, { onAction(status) }, enabled = !busy, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
