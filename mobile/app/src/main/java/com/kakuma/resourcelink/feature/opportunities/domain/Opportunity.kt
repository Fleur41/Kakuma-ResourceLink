package com.kakuma.resourcelink.feature.opportunities.domain

import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class OpportunityCategory(val apiValue: String) {
    SCHOLARSHIP("scholarship"), EDUCATION("education"), VOCATIONAL("vocational"),
    JOB("job"), INTERNSHIP("internship"), ENTREPRENEURSHIP("entrepreneurship");

    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } ?: EDUCATION }
}

data class Opportunity(
    val id: String,
    val title: String,
    val category: OpportunityCategory,
    val organization: String,
    val summary: String,
    val description: String,
    val requirements: List<String>,
    val location: String,
    val isRemote: Boolean,
    val deadline: LocalDate?,
    val applicationUrl: String,
    val isVerified: Boolean,
    val isBookmarked: Boolean,
)

data class OpportunityFilter(
    val category: OpportunityCategory? = null,
    val query: String = "",
    val bookmarkedOnly: Boolean = false,
    val openOnly: Boolean = true,
) {
    /** Stable cache key: each filter combination keeps its own offline list. */
    val listKey: String get() = "opp:${category?.apiValue ?: "all"}:${query.trim().lowercase()}:$bookmarkedOnly:$openOnly"
}

/** Result of a toggle that may complete later (offline queue). */
sealed interface SyncOutcome {
    data object Synced : SyncOutcome
    data object QueuedOffline : SyncOutcome
    data class Failed(val error: com.kakuma.resourcelink.core.common.AppError) : SyncOutcome
}

interface OpportunityRepository {
    fun paged(filter: OpportunityFilter): Flow<PagingData<Opportunity>>
    fun featured(limit: Int = 5): Flow<List<Opportunity>>
    suspend fun refreshFeatured(): AppResult<Unit>
    fun observe(id: String): Flow<Opportunity?>
    suspend fun refresh(id: String): AppResult<Opportunity>
    suspend fun setBookmarked(id: String, bookmarked: Boolean): SyncOutcome
}
