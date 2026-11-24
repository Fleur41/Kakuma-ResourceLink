package com.kakuma.resourcelink.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.connectivity.ConnectivityObserver
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.report
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.auth.domain.User
import com.kakuma.resourcelink.feature.notifications.domain.NotificationRepository
import com.kakuma.resourcelink.feature.opportunities.domain.Opportunity
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityRepository
import com.kakuma.resourcelink.feature.opportunities.domain.ToggleOpportunityBookmarkUseCase
import com.kakuma.resourcelink.feature.stories.domain.Story
import com.kakuma.resourcelink.feature.stories.domain.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: User? = null,
    val opportunities: List<Opportunity> = emptyList(),
    val stories: List<Story> = emptyList(),
    val unread: Int = 0,
    val isRefreshing: Boolean = false,
    /** Only shown when nothing is cached. */
    val error: UiText? = null,
    val isOnline: Boolean = true,
) {
    val isEmpty: Boolean get() = opportunities.isEmpty() && stories.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val opportunities: OpportunityRepository,
    private val stories: StoryRepository,
    private val notifications: NotificationRepository,
    private val toggleBookmark: ToggleOpportunityBookmarkUseCase,
    private val auth: AuthRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<UiText?>(null)
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    val state: StateFlow<HomeUiState> = combine(
        combine(auth.currentUser, notifications.unreadCount, ::Pair),
        opportunities.featured(),
        stories.featured(),
        combine(refreshing, error, ::Pair),
        connectivity.isOnline,
    ) { (user, unread), opps, st, (r, e), online ->
        HomeUiState(user, opps, st, unread, r, if (opps.isEmpty() && st.isEmpty()) e else null, online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isRefreshing = true))

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            val a = async { opportunities.refreshFeatured() }
            val b = async { stories.refreshFeatured() }
            if (auth.isLoggedIn.value) launch { notifications.refreshUnreadCount() }
            val failure = listOf(a.await(), b.await()).filterIsInstance<AppResult.Failure>().firstOrNull()
            error.value = failure?.error?.toUiText()
            refreshing.value = false
        }
    }

    fun onToggleBookmark(o: Opportunity) {
        viewModelScope.launch {
            uiEvents.report(toggleBookmark(o), if (o.isBookmarked) R.string.toast_bookmark_removed else R.string.toast_bookmarked)
        }
    }
}
