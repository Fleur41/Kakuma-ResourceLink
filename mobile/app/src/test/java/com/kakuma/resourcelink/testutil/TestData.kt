package com.kakuma.resourcelink.testutil

import com.kakuma.resourcelink.core.database.entity.OpportunityEntity
import com.kakuma.resourcelink.feature.auth.domain.User
import com.kakuma.resourcelink.feature.auth.domain.UserRole

object TestData {
    fun user(fullName: String = "Amina Hassan") = User(
        id = "u1",
        email = "amina@example.com",
        fullName = fullName,
        role = UserRole.MEMBER,
        preferredLanguage = "en",
        bio = "",
        location = "",
        avatarUrl = null,
        hasAlumniProfile = false,
    )

    fun opportunityEntity(id: String, listKey: String = "all", position: Int = 0, bookmarked: Boolean = false) =
        OpportunityEntity(
            listKey = listKey,
            id = id,
            position = position,
            title = "Opportunity $id",
            category = "scholarship",
            organization = "Org",
            summary = "Summary",
            description = "Description",
            requirements = listOf("One", "Two"),
            location = "Kakuma",
            isRemote = false,
            deadline = "2026-12-31",
            applicationUrl = "https://example.org/apply",
            isVerified = false,
            isBookmarked = bookmarked,
        )
}
