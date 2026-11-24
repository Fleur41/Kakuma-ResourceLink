package com.kakuma.resourcelink.feature.stories.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.connectivity.ConnectivityObserver
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.report
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.stories.domain.DraftRepository
import com.kakuma.resourcelink.feature.stories.domain.Story
import com.kakuma.resourcelink.feature.stories.domain.StoryCategory
import com.kakuma.resourcelink.feature.stories.domain.StoryDraft
import com.kakuma.resourcelink.feature.stories.domain.StoryFeed
import com.kakuma.resourcelink.feature.stories.domain.StoryFilter
import com.kakuma.resourcelink.feature.stories.domain.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val repository: StoryRepository,
    private val drafts: DraftRepository,
    authRepository: AuthRepository,
    connectivity: ConnectivityObserver,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    val filter: StateFlow<StoryFilter> = savedState.getStateFlow(KEY_FEED, StoryFeed.LATEST.name)
        .flatMapLatest { feed ->
            savedState.getStateFlow<String?>(KEY_CATEGORY, null).map { cat ->
                StoryFilter(StoryFeed.valueOf(feed), cat?.let(StoryCategory::from))
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoryFilter())

    val isLoggedIn = authRepository.isLoggedIn
    val isOnline = connectivity.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val localDrafts: StateFlow<List<StoryDraft>> = drafts.drafts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stories: Flow<PagingData<Story>> = filter.flatMapLatest { repository.paged(it) }.cachedIn(viewModelScope)

    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    fun selectFeed(feed: StoryFeed) { savedState[KEY_FEED] = feed.name }
    fun selectCategory(category: StoryCategory?) { savedState[KEY_CATEGORY] = category?.apiValue }

    fun toggleLike(story: Story) = viewModelScope.launch {
        uiEvents.report(repository.setLiked(story, !story.isLiked), successMessage = null)
    }

    fun toggleBookmark(story: Story) = viewModelScope.launch {
        uiEvents.report(
            repository.setBookmarked(story, !story.isBookmarked),
            if (story.isBookmarked) R.string.toast_bookmark_removed else R.string.toast_bookmarked,
        )
    }

    private companion object {
        const val KEY_FEED = "feed"
        const val KEY_CATEGORY = "category"
    }
}
