package com.kakuma.resourcelink.feature.projects.domain

import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.feature.stories.domain.Author
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

enum class ProjectCategory(val apiValue: String) {
    EDUCATION("education"), HEALTH("health"), LIVELIHOOD("livelihood"), TECHNOLOGY("technology"),
    ENVIRONMENT("environment"), YOUTH("youth"), OTHER("other");
    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } ?: OTHER }
}

enum class ContributionType(val apiValue: String) {
    MENTORING("mentoring"), VOLUNTEERING("volunteering"), EQUIPMENT("equipment"), SKILLS("skills"), FINANCIAL_PLEDGE("financial_pledge");
    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } }
}

data class ContributionSummary(
    val totalContributions: Int,
    val pledgedAmount: BigDecimal,
    val currency: String,
    val byType: Map<ContributionType, Int>,
    val note: String,
)

data class CommunityProject(
    val id: String,
    val owner: Author,
    val title: String,
    val summary: String,
    val description: String,
    val category: ProjectCategory,
    val location: String,
    val coverImage: String?,
    val status: String,
    val acceptedTypes: List<ContributionType>,
    val fundingGoal: BigDecimal?,
    val currency: String,
    val summaryStats: ContributionSummary,
) {
    /** 0..1 progress of pledged money towards the goal; null when the project has no money goal. */
    val pledgeProgress: Float? get() = fundingGoal?.takeIf { it > BigDecimal.ZERO }?.let {
        (summaryStats.pledgedAmount.toFloat() / it.toFloat()).coerceIn(0f, 1f)
    }
}

data class Contribution(val id: String, val type: ContributionType, val description: String, val amount: BigDecimal?, val currency: String, val status: String)

sealed interface PledgeValidation {
    data object Ok : PledgeValidation
    data object DescriptionTooShort : PledgeValidation
    data object AmountRequired : PledgeValidation
}

object PledgeRules {
    fun validate(type: ContributionType, description: String, amount: String): PledgeValidation = when {
        description.trim().length < 3 -> PledgeValidation.DescriptionTooShort
        type == ContributionType.FINANCIAL_PLEDGE && (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO -> PledgeValidation.AmountRequired
        else -> PledgeValidation.Ok
    }
}

interface ProjectRepository {
    fun projects(category: ProjectCategory?): Flow<PagingData<CommunityProject>>
    suspend fun get(id: String): AppResult<CommunityProject>
    suspend fun myContributions(projectId: String): AppResult<List<Contribution>>
    suspend fun pledge(projectId: String, type: ContributionType, description: String, amount: BigDecimal?): AppResult<Contribution>
}
