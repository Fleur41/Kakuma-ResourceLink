package com.kakuma.resourcelink.feature.opportunities.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.opportunities.domain.Opportunity
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityRepository
import com.kakuma.resourcelink.feature.opportunities.domain.SyncOutcome
import com.kakuma.resourcelink.feature.opportunities.domain.ToggleOpportunityBookmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpportunityDetailUiState(
    val opportunity: Opportunity? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
)

@HiltViewModel
class OpportunityDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: OpportunityRepository,
    private val toggleBookmark: ToggleOpportunityBookmarkUseCase,
) : ViewModel() {
    val id: String = checkNotNull(savedState["id"])
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<UiText?>(null)
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    val state: StateFlow<OpportunityDetailUiState> = combine(repository.observe(id), loading, error) { opp, l, e ->
        OpportunityDetailUiState(opp, l && opp == null, if (opp == null) e else null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OpportunityDetailUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            when (val r = repository.refresh(id)) {
                is AppResult.Success -> error.value = null
                // Cached copy (if any) stays visible offline; only show an error when there is nothing to show.
                is AppResult.Failure -> error.value = if (r.error == AppError.NotFound) UiText.Res(R.string.error_not_found) else r.error.toUiText()
            }
            loading.value = false
        }
    }

    fun onToggleBookmark() {
        val opp = state.value.opportunity ?: return
        viewModelScope.launch {
            when (val outcome = toggleBookmark(opp)) {
                SyncOutcome.Synced -> uiEvents.toast(UiText.Res(if (opp.isBookmarked) R.string.toast_bookmark_removed else R.string.toast_bookmarked))
                SyncOutcome.QueuedOffline -> uiEvents.toast(UiText.Res(R.string.toast_saved_offline_sync_later))
                is SyncOutcome.Failed -> uiEvents.snackbar(outcome.error.toUiText())
            }
        }
    }
}
