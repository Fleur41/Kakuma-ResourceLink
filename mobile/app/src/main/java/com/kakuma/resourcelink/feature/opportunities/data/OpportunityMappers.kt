package com.kakuma.resourcelink.feature.opportunities.data

import com.kakuma.resourcelink.core.database.entity.OpportunityEntity
import com.kakuma.resourcelink.feature.opportunities.domain.Opportunity
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityCategory
import java.time.LocalDate

fun OpportunityDto.toEntity(listKey: String, position: Int) = OpportunityEntity(
    listKey = listKey, id = id, position = position, title = title, category = category,
    organization = organization, summary = summary, description = description, requirements = requirements,
    location = location, isRemote = isRemote, deadline = deadline, applicationUrl = applicationUrl,
    isVerified = isVerified, isBookmarked = isBookmarked,
)

fun OpportunityEntity.toDomain() = Opportunity(
    id = id, title = title, category = OpportunityCategory.from(category), organization = organization,
    summary = summary, description = description, requirements = requirements, location = location,
    isRemote = isRemote, deadline = deadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    applicationUrl = applicationUrl, isVerified = isVerified, isBookmarked = isBookmarked,
)

fun OpportunityDto.toDomain() = toEntity(DETAIL_LIST, 0).toDomain()

/** Opportunities opened directly (e.g. from a notification or AI source) are cached under this key. */
const val DETAIL_LIST = "opp:detail"
const val FEATURED_LIST = "opp:featured"
