package com.kakuma.resourcelink.feature.opportunities.domain

import javax.inject.Inject

class ToggleOpportunityBookmarkUseCase @Inject constructor(private val repository: OpportunityRepository) {
    suspend operator fun invoke(opportunity: Opportunity): SyncOutcome =
        repository.setBookmarked(opportunity.id, !opportunity.isBookmarked)
}
