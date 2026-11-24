package com.kakuma.resourcelink.feature.services.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.connectivity.ConnectivityObserver
import com.kakuma.resourcelink.feature.services.domain.EssentialService
import com.kakuma.resourcelink.feature.services.domain.ServiceCategory
import com.kakuma.resourcelink.feature.services.domain.ServiceRepository
import com.kakuma.resourcelink.feature.services.domain.filterBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServicesUiState(
    val services: List<EssentialService> = emptyList(),
    val query: String = "",
    val category: ServiceCategory? = null,
    val isRefreshing: Boolean = false,
    val hasCache: Boolean = false,
    /** Only shown when there is nothing cached to display. */
    val error: UiText? = null,
    val isOnline: Boolean = true,
)

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val repository: ServiceRepository,
    connectivity: ConnectivityObserver,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<UiText?>(null)
    private val query = savedState.getStateFlow("q", "")
    private val category = savedState.getStateFlow<String?>("cat", null)

    val state: StateFlow<ServicesUiState> = combine(
        repository.services(), query, category, combine(refreshing, error, ::Pair), connectivity.isOnline,
    ) { all, q, cat, (r, e), online ->
        val c = cat?.let(ServiceCategory::from)
        ServicesUiState(all.filterBy(c, q), q, c, r, all.isNotEmpty(), if (all.isEmpty()) e else null, online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServicesUiState(isRefreshing = true))

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = (repository.refresh() as? AppResult.Failure)?.error?.toUiText()
            refreshing.value = false
        }
    }

    fun onQueryChange(q: String) { savedState["q"] = q }
    fun onCategoryChange(c: ServiceCategory?) { savedState["cat"] = c?.apiValue }
}
