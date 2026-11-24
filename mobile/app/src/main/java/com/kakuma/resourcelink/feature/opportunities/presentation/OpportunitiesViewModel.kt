package com.kakuma.resourcelink.feature.opportunities.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.connectivity.ConnectivityObserver
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.feature.opportunities.domain.Opportunity
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityCategory
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityFilter
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityRepository
import com.kakuma.resourcelink.feature.opportunities.domain.SyncOutcome
import com.kakuma.resourcelink.feature.opportunities.domain.ToggleOpportunityBookmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpportunitiesUiState(
    val query: String = "",
    val category: OpportunityCategory? = null,
    val bookmarkedOnly: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class OpportunitiesViewModel @Inject constructor(
    private val repository: OpportunityRepository,
    private val toggleBookmark: ToggleOpportunityBookmarkUseCase,
    connectivity: ConnectivityObserver,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OpportunitiesUiState(
            category = savedState.get<String>("category")?.let(OpportunityCategory::from),
            bookmarkedOnly = savedState.get<Boolean>("bookmarked") ?: false,
        ),
    )
    val state: StateFlow<OpportunitiesUiState> = _state.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivity.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    val opportunities: Flow<PagingData<Opportunity>> = _state
        .debounce(300) // avoid a request per keystroke
        .distinctUntilChanged()
        .flatMapLatest { s -> repository.paged(OpportunityFilter(s.category, s.query, s.bookmarkedOnly, openOnly = !s.bookmarkedOnly)) }
        .cachedIn(viewModelScope)

    fun onQueryChange(q: String) = _state.update { it.copy(query = q) }
    fun onCategoryChange(c: OpportunityCategory?) = _state.update { it.copy(category = c) }

    fun onToggleBookmark(opportunity: Opportunity) {
        viewModelScope.launch {
            when (val outcome = toggleBookmark(opportunity)) {
                SyncOutcome.Synced -> uiEvents.toast(
                    UiText.Res(if (opportunity.isBookmarked) R.string.toast_bookmark_removed else R.string.toast_bookmarked),
                )
                SyncOutcome.QueuedOffline -> uiEvents.toast(UiText.Res(R.string.toast_saved_offline_sync_later))
                is SyncOutcome.Failed -> uiEvents.snackbar(outcome.error.toUiText())
            }
        }
    }
}
